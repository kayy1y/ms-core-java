// Exporta los datos semilla de la Fase I (createSeed() de los mocks) para MS1:
//   src/main/resources/db/migration/V2__seed_fase1.sql   usuarios, disponibilidad, sesiones, insignias
//   src/main/resources/stub/catalog.json                 servicios y habilidades para el catálogo "stub"
//
// Se corre desde la carpeta del frontend de la Fase I (para que resuelva el alias @/):
//   cd "<fase1 front>"
//   SEED_HASH='<bcrypt de Skill123!>' npx tsx --tsconfig tsconfig.json <ms-core-java>/tools/seed/export-seed.mts <ms-core-java>
//
// Los ids se conservan (usr_001, srv_014, ses_120…) para que coincidan con el seed de MS2.
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'

const [outRoot] = process.argv.slice(2)
const hash = process.env.SEED_HASH
if (!outRoot || !hash) {
  console.error('Uso: SEED_HASH=<bcrypt> npx tsx export-seed.mts <ruta a ms-core-java>')
  process.exit(1)
}

const seedModule = path.resolve('src/mocks/fixtures/seed.ts')
const { createSeed } = await import(seedModule)
const db = createSeed()

const ms = (iso: string) => {
  const t = Date.parse(iso)
  if (Number.isNaN(t)) throw new Error(`Fecha inválida: ${iso}`)
  return t
}
const q = (v: string | null | undefined) => (v === null || v === undefined ? 'NULL' : `N'${v.replace(/'/g, "''")}'`)
const n = (v: number | null | undefined) => (v === null || v === undefined ? 'NULL' : String(v))
const lines: string[] = [
  '-- Generado por tools/seed/export-seed.mts a partir de createSeed() de la Fase I. No editar a mano.',
  '-- Contraseña de todos los usuarios semilla: Skill123!',
  '',
]
const insert = (table: string, cols: string[], rows: string[][]) => {
  if (rows.length === 0) return
  for (let i = 0; i < rows.length; i += 500) {
    lines.push(`INSERT INTO ${table} (${cols.join(', ')}) VALUES`)
    lines.push(rows.slice(i, i + 500).map((r) => `  (${r.join(', ')})`).join(',\n') + ';')
  }
  lines.push('')
}

// ---------- Usuarios ----------
insert(
  'users',
  ['id', 'full_name', 'email', 'password_hash', 'google_sub', 'avatar_url', 'bio', 'role', 'status',
    'academic_institution', 'academic_career', 'academic_level', 'professional_occupation',
    'professional_experience', 'knowledge', 'email_preferences', 'created_at_epoch_ms', 'updated_at_epoch_ms'],
  db.users.map((u) => [
    q(u.id), q(u.fullName), q(u.email), q(hash), 'NULL', q(u.avatarUrl), q(u.bio ?? ''), q(u.role), q(u.status),
    q(u.academic?.institution), q(u.academic?.career), q(u.academic?.level),
    q(u.professional?.occupation), q(u.professional?.experience),
    q(JSON.stringify(u.knowledge)), q(JSON.stringify(u.emailPreferences)),
    n(ms(u.createdAt)), n(ms(u.createdAt)),
  ]),
)
insert(
  'user_teach_skills',
  ['user_id', 'position', 'skill_id', 'name', 'level'],
  db.users.flatMap((u) => u.teachSkills.map((t, i) => [q(u.id), n(i), q(t.skillId), q(t.name), q(t.level)])),
)
insert(
  'user_learning_interests',
  ['user_id', 'position', 'skill_id', 'name'],
  db.users.flatMap((u) => u.learningInterests.map((t, i) => [q(u.id), n(i), q(t.skillId), q(t.name)])),
)

// ---------- Disponibilidad ----------
const avail = Object.entries(db.availability)
insert(
  'availability_rules',
  ['id', 'mentor_id', 'day_of_week'],
  avail.flatMap(([mentorId, a]) => a.rules.map((r) => [q(r.id), q(mentorId), n(r.dayOfWeek)])),
)
insert(
  'availability_rule_services',
  ['rule_id', 'position', 'service_id'],
  avail.flatMap(([, a]) => a.rules.flatMap((r) => r.serviceIds.map((s, i) => [q(r.id), n(i), q(s)]))),
)
insert(
  'blocked_dates',
  ['id', 'mentor_id', 'blocked_on', 'reason'],
  avail.flatMap(([mentorId, a]) => a.blockedDates.map((b) => [q(b.id), q(mentorId), q(b.date), q(b.reason)])),
)

// ---------- Sesiones ----------
const services = new Map(db.services.map((s) => [s.id, s]))
const activeDays = new Set<string>()
let skipped = 0
const sessions = db.sessions.filter((s) => {
  if (!['pendiente', 'confirmada', 'reprogramada'].includes(s.status)) return true
  const key = `${s.serviceId}|${s.date}`
  if (activeDays.has(key)) {
    skipped++ // el índice único no admite dos sesiones activas del mismo servicio el mismo día
    return false
  }
  activeDays.add(key)
  return true
})
insert(
  'sessions',
  ['id', 'service_id', 'service_title', 'service_duration_minutes', 'service_type', 'service_capacity',
    'service_modality', 'mentor_id', 'session_date', 'modality', 'status', 'platform', 'other_platform_name',
    'meeting_link', 'location_name', 'location_address', 'location_notes', 'learner_note', 'proposed_by',
    'cancel_reason', 'cancel_comment', 'created_at_epoch_ms', 'version'],
  sessions.map((s) => {
    const svc = services.get(s.serviceId)
    if (!svc) throw new Error(`La sesión ${s.id} apunta a un servicio inexistente ${s.serviceId}`)
    return [
      q(s.id), q(svc.id), q(svc.title), n(svc.durationMinutes), q(svc.type), n(svc.capacity), q(svc.modality),
      q(s.mentorId), q(s.date), q(s.modality), q(s.status), q(s.platform), q(s.otherPlatformName ?? null),
      q(s.meetingLink), q(s.location?.name), q(s.location?.address), q(s.location?.notes),
      q(s.learnerNote ?? null), q(s.proposedBy ?? null), q(s.cancelReason ?? null), q(s.cancelComment ?? null),
      n(ms(s.createdAt)), '0',
    ]
  }),
)
insert(
  'session_learners',
  ['session_id', 'position', 'learner_id'],
  sessions.flatMap((s) => s.learnerIds.map((l, i) => [q(s.id), n(i), q(l)])),
)
insert(
  'session_history',
  ['session_id', 'position', 'status', 'at_epoch_ms', 'by_user_id', 'comment'],
  sessions.flatMap((s) => s.history.map((h, i) => [q(s.id), n(i), q(h.status), n(ms(h.at)), q(h.by), q(h.comment ?? null)])),
)

// ---------- Insignias ----------
insert(
  'badges',
  ['id', 'name', 'description', 'icon', 'criterion', 'threshold', 'min_reviews', 'active'],
  db.badges.map((b) => [q(b.id), q(b.name), q(b.description), q(b.icon), q(b.criterion), n(b.threshold),
    n(b.minReviews ?? null), b.active ? '1' : '0']),
)
insert(
  'user_badges',
  ['user_id', 'badge_id', 'earned_at_epoch_ms', 'reason'],
  db.userBadges.map((ub) => [q(ub.userId), q(ub.badgeId), n(ms(ub.earnedAt)), q(ub.reason)]),
)

const migration = path.join(outRoot, 'src/main/resources/db/migration/V2__seed_fase1.sql')
writeFileSync(migration, lines.join('\n'))

// ---------- Catálogo stub (lo que normalmente responde MS2) ----------
const catalog = {
  services: db.services
    .filter((s) => !s.deleted)
    .map((s) => ({
      id: s.id, mentorId: s.mentorId, title: s.title, durationMinutes: s.durationMinutes, type: s.type,
      capacity: s.capacity, modality: s.modality, status: s.status,
    })),
  skills: db.skills.map((s) => ({ id: s.id, name: s.name })),
}
mkdirSync(path.join(outRoot, 'src/main/resources/stub'), { recursive: true })
writeFileSync(path.join(outRoot, 'src/main/resources/stub/catalog.json'), JSON.stringify(catalog, null, 2) + '\n')

console.log(
  `usuarios=${db.users.length} sesiones=${sessions.length} (omitidas por choque de día: ${skipped}) ` +
    `servicios=${catalog.services.length} habilidades=${catalog.skills.length} insignias=${db.badges.length}`,
)
