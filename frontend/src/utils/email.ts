/** Extract display name from "Name <email>" or return the string as-is. */
export function displayNameFromFrom(from: string | null | undefined): string {
  if (from == null || from.trim() === '') return 'Unknown'
  const trimmed = from.trim()
  const match = trimmed.match(/^(.+?)\s*<[^>]+>$/)
  return (match?.[1]?.trim() ?? trimmed) || 'Unknown'
}
