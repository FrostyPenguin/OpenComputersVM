
function checkArg(n, have, ...)
    have = type(have)
    local function check(want, ...)
        if not want then
            return false
        else
            return have == want or check(...)
        end
    end
    
    if not check(...) then
        error(string.format("bad argument #%d (%s expected, got %s)", n, table.concat({...}, " or "), have), 3)
    end
end

---------------------------------------- EEPROM loading ----------------------------------------

local eeprom = component.list("eeprom")()
if eeprom then
    eeprom = component.proxy(eeprom)
    
    local result, reason = load(eeprom.get(), "=EEPROM")
    if result then
        result, reason = xpcall(result, debug.traceback)
        if not result then
             error("Failed to call EEPROM code: " .. tostring(reason))
        end
    else
        error("Failed to load EEPROM code: " .. tostring(reason))
    end
else
    error("install configured EEPROM")
end