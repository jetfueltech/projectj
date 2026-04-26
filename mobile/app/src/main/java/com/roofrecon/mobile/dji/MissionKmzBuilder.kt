package com.roofrecon.mobile.dji

import com.roofrecon.mobile.net.Waypoint
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Build a DJI WPML KMZ from our backend's waypoint list.
 *
 * DJI MSDK v5 consumes Waypoint Markup Language (WPML) packaged as a `.kmz`
 * archive containing `wpmz/template.kml` and `wpmz/waylines.wpml`. The minimal
 * structure below covers a "shoot photo at each waypoint" mission. The real
 * spec is documented at:
 *   https://developer.dji.com/doc/cloud-api-tutorial/en/specification/dji-wpml/overview.html
 */
object MissionKmzBuilder {

    fun build(jobId: String, waypoints: List<Waypoint>): File {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "missions")
        tmpDir.mkdirs()
        val out = File(tmpDir, "$jobId.kmz")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry("wpmz/template.kml"))
            zip.write(template().toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("wpmz/waylines.wpml"))
            zip.write(waylines(waypoints).toByteArray())
            zip.closeEntry()
        }
        return out
    }

    private fun template(): String = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:wpml="http://www.dji.com/wpmz/1.0.2">
  <Document>
    <wpml:author>RoofRecon</wpml:author>
    <wpml:missionConfig>
      <wpml:flyToWaylineMode>safely</wpml:flyToWaylineMode>
      <wpml:finishAction>goHome</wpml:finishAction>
      <wpml:exitOnRCLost>goContinue</wpml:exitOnRCLost>
      <wpml:globalTransitionalSpeed>5</wpml:globalTransitionalSpeed>
      <wpml:droneInfo>
        <wpml:droneEnumValue>77</wpml:droneEnumValue>
        <wpml:droneSubEnumValue>0</wpml:droneSubEnumValue>
      </wpml:droneInfo>
    </wpml:missionConfig>
  </Document>
</kml>"""

    private fun waylines(wps: List<Waypoint>): String {
        val placemarks = wps.joinToString("\n") { wp ->
            """      <Placemark>
        <Point><coordinates>${wp.longitude},${wp.latitude}</coordinates></Point>
        <wpml:index>${wp.ordering}</wpml:index>
        <wpml:executeHeight>${wp.altitudeM}</wpml:executeHeight>
        <wpml:waypointSpeed>${wp.speedMs ?: 4.0}</wpml:waypointSpeed>
        <wpml:waypointHeadingParam>
          <wpml:waypointHeadingMode>smoothTransition</wpml:waypointHeadingMode>
          <wpml:waypointHeadingAngle>${wp.headingDeg ?: 0.0}</wpml:waypointHeadingAngle>
        </wpml:waypointHeadingParam>
        <wpml:waypointGimbalHeadingParam>
          <wpml:waypointGimbalPitchAngle>${wp.gimbalPitchDeg ?: -90.0}</wpml:waypointGimbalPitchAngle>
        </wpml:waypointGimbalHeadingParam>
        <wpml:actionGroup>
          <wpml:actionGroupId>${wp.ordering}</wpml:actionGroupId>
          <wpml:actionGroupStartIndex>${wp.ordering}</wpml:actionGroupStartIndex>
          <wpml:actionGroupEndIndex>${wp.ordering}</wpml:actionGroupEndIndex>
          <wpml:actionGroupMode>sequence</wpml:actionGroupMode>
          <wpml:actionTrigger>
            <wpml:actionTriggerType>reachPoint</wpml:actionTriggerType>
          </wpml:actionTrigger>
          <wpml:action>
            <wpml:actionId>1</wpml:actionId>
            <wpml:actionActuatorFunc>takePhoto</wpml:actionActuatorFunc>
          </wpml:action>
        </wpml:actionGroup>
      </Placemark>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:wpml="http://www.dji.com/wpmz/1.0.2">
  <Document>
    <Folder>
      <wpml:templateId>0</wpml:templateId>
      <wpml:waylineId>0</wpml:waylineId>
      <wpml:autoFlightSpeed>5</wpml:autoFlightSpeed>
$placemarks
    </Folder>
  </Document>
</kml>"""
    }
}
