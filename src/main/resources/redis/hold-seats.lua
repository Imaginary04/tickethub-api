-- Holds ALL the given seats or NONE of them.
-- Redis runs a script from start to end without running anything else in between,
-- so no other request can grab a seat between the check and the SET.
--
-- KEYS    one key per seat
-- ARGV[1] value to store (who holds it)
-- ARGV[2] how long the hold lasts, in milliseconds
-- Returns the positions (1, 2, ...) of seats that are already held; an empty list means success.

local taken = {}
for i, key in ipairs(KEYS) do
  if redis.call('EXISTS', key) == 1 then
    table.insert(taken, i)
  end
end

if #taken > 0 then
  return taken
end

for _, key in ipairs(KEYS) do
  redis.call('SET', key, ARGV[1], 'PX', ARGV[2])
end
return taken