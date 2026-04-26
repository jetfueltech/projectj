CREATE TABLE IF NOT EXISTS "Damage" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"imageId" uuid,
	"planeId" uuid,
	"type" varchar NOT NULL,
	"severity" integer NOT NULL,
	"confidence" double precision,
	"bbox" json,
	"geo" json,
	"notes" text
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Image" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"kind" varchar NOT NULL,
	"blobUrl" text NOT NULL,
	"filename" varchar(256),
	"width" integer,
	"height" integer,
	"latitude" double precision,
	"longitude" double precision,
	"altitude" double precision,
	"yaw" double precision,
	"pitch" double precision,
	"roll" double precision,
	"gimbalPitch" double precision,
	"capturedAt" timestamp,
	"createdAt" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Job" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"propertyId" uuid NOT NULL,
	"userId" uuid NOT NULL,
	"status" varchar DEFAULT 'created' NOT NULL,
	"droneSerial" varchar(64),
	"droneModel" varchar(64),
	"errorMessage" text,
	"createdAt" timestamp DEFAULT now() NOT NULL,
	"updatedAt" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Obstacle" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"kind" varchar(32) NOT NULL,
	"geometry" json NOT NULL,
	"heightM" double precision
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Property" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"userId" uuid NOT NULL,
	"customerName" varchar(128) NOT NULL,
	"customerPhone" varchar(32),
	"customerEmail" varchar(128),
	"addressLine1" varchar(256) NOT NULL,
	"addressLine2" varchar(256),
	"city" varchar(128) NOT NULL,
	"state" varchar(64) NOT NULL,
	"postalCode" varchar(16) NOT NULL,
	"latitude" double precision,
	"longitude" double precision,
	"notes" text,
	"createdAt" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Report" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"pdfUrl" text,
	"summary" text,
	"totals" json,
	"createdAt" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "RoofPlane" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"polygon" json NOT NULL,
	"areaSqFt" double precision,
	"pitchDegrees" double precision,
	"azimuthDegrees" double precision,
	"ridgeHeightM" double precision,
	"eaveHeightM" double precision
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "User" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"email" varchar(128) NOT NULL,
	"password" varchar(128),
	"name" varchar(128),
	"company" varchar(128),
	"createdAt" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS "Waypoint" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"jobId" uuid NOT NULL,
	"ordering" integer NOT NULL,
	"latitude" double precision NOT NULL,
	"longitude" double precision NOT NULL,
	"altitudeM" double precision NOT NULL,
	"headingDeg" double precision,
	"gimbalPitchDeg" double precision,
	"speedMs" double precision DEFAULT 4,
	"action" varchar(32) DEFAULT 'shoot_photo'
);
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Damage" ADD CONSTRAINT "Damage_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Damage" ADD CONSTRAINT "Damage_imageId_Image_id_fk" FOREIGN KEY ("imageId") REFERENCES "public"."Image"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Damage" ADD CONSTRAINT "Damage_planeId_RoofPlane_id_fk" FOREIGN KEY ("planeId") REFERENCES "public"."RoofPlane"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Image" ADD CONSTRAINT "Image_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Job" ADD CONSTRAINT "Job_propertyId_Property_id_fk" FOREIGN KEY ("propertyId") REFERENCES "public"."Property"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Job" ADD CONSTRAINT "Job_userId_User_id_fk" FOREIGN KEY ("userId") REFERENCES "public"."User"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Obstacle" ADD CONSTRAINT "Obstacle_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Property" ADD CONSTRAINT "Property_userId_User_id_fk" FOREIGN KEY ("userId") REFERENCES "public"."User"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Report" ADD CONSTRAINT "Report_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "RoofPlane" ADD CONSTRAINT "RoofPlane_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
--> statement-breakpoint
DO $$ BEGIN
 ALTER TABLE "Waypoint" ADD CONSTRAINT "Waypoint_jobId_Job_id_fk" FOREIGN KEY ("jobId") REFERENCES "public"."Job"("id") ON DELETE no action ON UPDATE no action;
EXCEPTION
 WHEN duplicate_object THEN null;
END $$;
