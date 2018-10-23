
---------------------------------------- Global ----------------------------------------

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

---------------------------------------- Sandbox ----------------------------------------

local sandbox = {}
for key, value in pairs(_G) do
	sandbox[key] = value
	--print(key, value)
end

sandbox.io = nil
sandbox.dofile = nil
sandbox._G = sandbox
sandbox.collectgarbage = nil
sandbox.eris = nil
sandbox.print = nil
sandbox.os.execute = nil
sandbox.os.remove = nil
sandbox.os.rename = nil
sandbox.os.exit = nil
sandbox.os.tmpname = nil

sandbox.load = function(ld, source, mode, env)
	return load(ld, source, mode, env or sandbox)
end

local sandboxComponentAPI, sandboxComputerAPI = sandbox.component, sandbox.computer

local oldList = component.list
function sandboxComponentAPI.list(filter, exact)
	local list, key = oldList(filter, exact)
	return setmetatable(list, {
		__call = function()
			key = next(list, key)
			if key then
				return key, list[key]
			end
		end
	})
end

function sandboxComponentAPI.invoke(address, method, ...)
	checkArg(1, address, "string")
	checkArg(2, method, "string")
	
	local proxy = sandboxComponentAPI.proxy(address)
	if proxy then
		if type(proxy[method]) == "function" then
			return proxy[method](...)
		else
			return false, "no such method"
		end
	else
		return false, "no such component"
	end
end

local computerComponent = sandboxComponentAPI.proxy(sandboxComponentAPI.list("computer")())
sandboxComputerAPI.beep = computerComponent.beep


-- Да пошли вы на хуй со своим кастомным говнищем. Че за дела? Хули под msvc компилером эта ебала вообще отсутствует? Пизда ноль унификации
local oldOSDate = os.date
sandbox.os.date = function(format, time)
    format = format:gsub("%%T", "%%X"):gsub("%%F", "%%Y-%%m-%%d")
	return oldOSDate(format, time)
end

---------------------------------------- EEPROM loading ----------------------------------------

local eeprom = sandboxComponentAPI.list("eeprom")()
if eeprom then
	eeprom = sandboxComponentAPI.proxy(eeprom)
	
	local result, reason = load(eeprom.get(), "=EEPROM", "t", sandbox)
	if result then
		result, reason = xpcall(result, debug.traceback)
		if not result then
			 error(tostring(reason))
		end
	else
		error(tostring(reason))
	end
else
	error("install configured EEPROM")
end