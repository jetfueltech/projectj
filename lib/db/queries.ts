import 'server-only';

import { genSaltSync, hashSync } from 'bcrypt-ts';
import { and, desc, eq } from 'drizzle-orm';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';

import {
  user,
  property,
  job,
  image,
  waypoint,
  roofPlane,
  obstacle,
  damage,
  report,
  type User,
  type Property,
  type Job,
  type Image,
  type Waypoint,
  type RoofPlane,
  type Obstacle,
  type Damage,
  type Report,
  type JobStatus,
} from './schema';

const client = postgres(process.env.POSTGRES_URL!);
const db = drizzle(client);

// ----- Users / auth -----

export async function getUser(email: string): Promise<Array<User>> {
  return db.select().from(user).where(eq(user.email, email));
}

export async function getUserById(id: string): Promise<User | undefined> {
  const [u] = await db.select().from(user).where(eq(user.id, id));
  return u;
}

export async function createUser(email: string, password: string) {
  const salt = genSaltSync(10);
  const hash = hashSync(password, salt);
  return db.insert(user).values({ email, password: hash });
}

// ----- Properties -----

export async function createProperty(input: Omit<Property, 'id' | 'createdAt'>) {
  const [row] = await db.insert(property).values(input).returning();
  return row;
}

export async function listProperties(userId: string) {
  return db
    .select()
    .from(property)
    .where(eq(property.userId, userId))
    .orderBy(desc(property.createdAt));
}

export async function getProperty(id: string, userId: string) {
  const [row] = await db
    .select()
    .from(property)
    .where(and(eq(property.id, id), eq(property.userId, userId)));
  return row;
}

// ----- Jobs -----

export async function createJob(input: {
  propertyId: string;
  userId: string;
  droneSerial?: string;
  droneModel?: string;
}) {
  const [row] = await db.insert(job).values(input).returning();
  return row;
}

export async function listJobsForUser(userId: string) {
  return db
    .select()
    .from(job)
    .where(eq(job.userId, userId))
    .orderBy(desc(job.createdAt));
}

export async function listJobsForProperty(propertyId: string, userId: string) {
  return db
    .select()
    .from(job)
    .where(and(eq(job.propertyId, propertyId), eq(job.userId, userId)))
    .orderBy(desc(job.createdAt));
}

export async function getJob(id: string): Promise<Job | undefined> {
  const [row] = await db.select().from(job).where(eq(job.id, id));
  return row;
}

export async function updateJobStatus(
  id: string,
  status: JobStatus,
  errorMessage?: string,
) {
  return db
    .update(job)
    .set({ status, errorMessage: errorMessage ?? null, updatedAt: new Date() })
    .where(eq(job.id, id));
}

// ----- Images -----

export async function insertImage(row: Omit<Image, 'id' | 'createdAt'>) {
  const [r] = await db.insert(image).values(row).returning();
  return r;
}

export async function listImages(
  jobId: string,
  kind?: Image['kind'],
): Promise<Array<Image>> {
  if (kind) {
    return db
      .select()
      .from(image)
      .where(and(eq(image.jobId, jobId), eq(image.kind, kind)));
  }
  return db.select().from(image).where(eq(image.jobId, jobId));
}

// ----- Planes / obstacles -----

export async function replacePlanes(
  jobId: string,
  planes: Array<Omit<RoofPlane, 'id' | 'jobId'>>,
) {
  await db.delete(roofPlane).where(eq(roofPlane.jobId, jobId));
  if (planes.length === 0) return [];
  return db
    .insert(roofPlane)
    .values(planes.map((p) => ({ ...p, jobId })))
    .returning();
}

export async function listPlanes(jobId: string) {
  return db.select().from(roofPlane).where(eq(roofPlane.jobId, jobId));
}

export async function replaceObstacles(
  jobId: string,
  obstacles: Array<Omit<Obstacle, 'id' | 'jobId'>>,
) {
  await db.delete(obstacle).where(eq(obstacle.jobId, jobId));
  if (obstacles.length === 0) return [];
  return db
    .insert(obstacle)
    .values(obstacles.map((o) => ({ ...o, jobId })))
    .returning();
}

// ----- Waypoints -----

export async function replaceWaypoints(
  jobId: string,
  waypoints: Array<Omit<Waypoint, 'id' | 'jobId'>>,
) {
  await db.delete(waypoint).where(eq(waypoint.jobId, jobId));
  if (waypoints.length === 0) return [];
  return db
    .insert(waypoint)
    .values(waypoints.map((w) => ({ ...w, jobId })))
    .returning();
}

export async function listWaypoints(jobId: string) {
  return db
    .select()
    .from(waypoint)
    .where(eq(waypoint.jobId, jobId))
    .orderBy(waypoint.ordering);
}

// ----- Damage / reports -----

export async function insertDamages(rows: Array<Omit<Damage, 'id'>>) {
  if (rows.length === 0) return [];
  return db.insert(damage).values(rows).returning();
}

export async function listDamages(jobId: string) {
  return db.select().from(damage).where(eq(damage.jobId, jobId));
}

export async function upsertReport(input: Omit<Report, 'id' | 'createdAt'>) {
  await db.delete(report).where(eq(report.jobId, input.jobId));
  const [row] = await db.insert(report).values(input).returning();
  return row;
}

export async function getReport(jobId: string) {
  const [row] = await db.select().from(report).where(eq(report.jobId, jobId));
  return row;
}
