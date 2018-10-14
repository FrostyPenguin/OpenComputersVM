
component.proxy(component.list("computer")()).beep(1500, 0.3)

local gpu = component.proxy(component.list("gpu")())

local width, height = gpu.getResolution()
local x, y = 3, 2

while true do
  local e = {computer.pullSignal()}
  print("Signal: ", table.unpack(e))
    
  if e[1] == "key_down" then
    if e[4] == 14 then
      gpu.setBackground(math.random(0xFFFFFF))
      gpu.setForeground(0xFFFFFF)      
      gpu.fill(1, 1, width, height, " ")
      x, y = 3, 2
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
    gpu.set(e[3], e[4], "#")
  end
end