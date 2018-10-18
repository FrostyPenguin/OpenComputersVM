
component.proxy(component.list("computer")()).beep(1500, 0.2)

print("Global environment:")
for key, value in pairs(_G) do
  print(key, value)
end

-- print("\nUNICODE TEST")
-- print(unicode.sub("Приветик, мирок", 3))
-- print(unicode.sub("Приветик, мирок", 3, 10))
-- print(unicode.sub("Приветик, мирок", 3, 4))
-- print(unicode.sub("Приветик, мирок", 3, 6))
-- print(unicode.sub("Приветик, мирок", 3, -1))
-- print(unicode.sub("Приветик, мирок", 3, -4))
-- print(unicode.sub("Приветик, мирок", 3, 100))

print("\nFS TEST")
local fs = component.proxy(component.list("filesystem")())

print("\nOpening for writing")
local id = fs.open("test.lua", "w")
print("handle id", id)
print("writing", fs.write(id, "hehehe"))
print("closing", fs.close(id))

print("\nOpening for reading")
local id = fs.open("test.lua", "r")
print("handle id", id)
print("reading", fs.read(id, 1000))
print("closing", fs.close(id))

print("\nExists:")
print(fs.exists("meow"))
print(fs.exists("TestDir1/"))

print("\nFile list:")
for key, value in pairs(fs.list("")) do
  print(key, value)
end

print("\nMODEM TEST")
local modem = component.proxy(component.list("modem")())
local port = 512
print(modem.open(port))
print(modem.broadcast(port, "meow", 123, "hehe", true))

print("\nGPU TEST")
local gpu = component.proxy(component.list("gpu")())

local width, height = gpu.getResolution()
local x, y

local function clear(b, f)
  gpu.setBackground(b)
  gpu.setForeground(f)
  gpu.fill(1, 1, width, height, " ")
  gpu.set(3, 2, "Hello world, пидор")
  
  x, y = 3, 4
end

clear(0x2D2D2D, 0xFFFFFF)

while true do
  local e = {computer.pullSignal()}
  print("RAM: ", computer.freeMemory() / 1024 / 1024, "Signal: ", table.unpack(e))
    
  if e[1] == "key_down" then
    if e[4] == 14 then
      clear(math.random(0xFFFFFF), 0xFFFFFF)
    elseif e[4] == 28 then
      x, y = 3, y + 1
    else
      local char = unicode.char(e[3])
      -- print("Lua char:", char)
      if char:match("[^\29\219\56\42\15\58\56\28\14]") then
        gpu.set(x, y, char)
        x = x + 1
      end
    end
  elseif e[1] == "touch" or e[1] == "drag" then
    gpu.set(e[3], e[4], "█")
  end
end