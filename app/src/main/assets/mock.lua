-- __ARITH_HOOK
do
  local _sm = setmetatable
  local z = function() return 0 end
  local f = function() return false end
  local c = function(a, b) return tostring(a) .. tostring(b) end
  local ops = {__add=z, __sub=z, __mul=z, __div=z, __mod=z, __pow=z, __unm=z, __lt=f, __le=f, __concat=c}
  setmetatable = function(t, mt)
    if type(mt) == 'table' and mt.__index ~= nil then
      for k, v in pairs(ops) do
        if rawget(mt, k) == nil then rawset(mt, k, v) end
      end
    end
    return _sm(t, mt)
  end
end
-- mock.lua (versi bersih) -- pakai: luajit mock.lua script.lua [detik_virtual]
-- Script tepercaya yang perlu menulis file: MOCK_TRUST=1 luajit mock.lua script.lua
-- File pendukung di ~ : api_dump.txt. Hasil: ~/dump_<nama>.txt dan ~/dec_N.lua
local ffi=require'ffi'
local unpack=table.unpack or unpack
local path,LIM=arg[1],(tonumber(arg[2]) or 10)*1000
if not path then print('pakai: luajit mock.lua script.lua [detik]') os.exit(1) end
local HOME=os.getenv('HOME') or '.'
local TRUST=os.getenv('MOCK_TRUST')=='1'
local CANARY='P@ssw0rd'
local URLPAT="https?://[^%s'\"\\<>]+"
local SKIPK={RPC=1,EVENT=1,FRAME=1,CMD=1}
local LOG,CNT,LC,UNK,MODS,CDEF,CMDS,FR,errs={},{},{},{},{},{},{},{},{}
local LEAK,URLS,NB={},{},0
local events={}
local NOSTUB={bit32=1,utf8=1,jit=1,newproxy=1,getfenv=1,setfenv=1,bit=1,unpack=1,loadstring=1,load=1,Enum=1,game=1,workspace=1,script=1,tick=1,typeof=1,task=1,syn=1,getgenv=1,identifyexecutor=1}
local T=0

------------------------------------------------------------------
-- LOG
------------------------------------------------------------------
local function ser(v)
  local t=type(v)
  if t=='string' then return ('%q'):format(v) end
  if t=='table' or t=='cdata' or t=='function' then return '<'..t..'>' end
  return tostring(v)
end
local function log(kind,name,...)
  local a={}
  for i=1,select('#',...) do a[i]=ser((select(i,...))) end
  local s=table.concat(a,', ')
  CNT[name]=(CNT[name] or 0)+1
  LC[name]=(LC[name] or 0)+1
  if kind=='BLOCK' then NB=NB+1 end
  local hot=false
  if not SKIPK[kind] then
    -- input palsu muncul di panggilan keluar = tanda keylog
    if s:find(CANARY,1,true) then LEAK[name]=true hot=LC[name]<=50 end
    if s:find('http',1,true) then for u in s:gmatch(URLPAT) do URLS[u]=true end end
  end
  -- tiap nama API maksimal 200 baris di TRACE (hitungan tetap lengkap di RINGKASAN)
  if hot or kind=='BLOCK' or (LC[name]<=200 and #LOG<50000) then
    LOG[#LOG+1]=('[%6.2f] %s %s(%s)'):format(T/1000,kind,name,s)
  end
end
local function U(name)
  return setmetatable({},{
    __call=function(_,...) log('CALL',name,...) return U(name..'()') end,
    __index=function(_,k) return U(name..'.'..tostring(k)) end,
    __newindex=function() end,
    __tostring=function() return name end,
    __len=function() return 0 end,
    __add=function() return 0 end,__sub=function() return 0 end,
    __mul=function() return 0 end,__div=function() return 0 end,
    __mod=function() return 0 end,__unm=function() return 0 end,
    __concat=function(a,b) return tostring(a)..tostring(b) end,
    __lt=function() return false end,__le=function() return false end,
  })
end

------------------------------------------------------------------
-- DATA PALSU (nilai return per API)
------------------------------------------------------------------
local D=path:match('^(.*)/[^/]+$') or '.'
local DEF={getAllChars={{}},getAllVehicles={{}},getAllObjects={{}},isSampAvailable={true},isSampLoaded={true},sampIsLocalPlayerSpawned={true},
  sampGetPlayerIdByCharHandle={true,0},sampGetPlayerNickname={'Mock_Player'},
  sampGetCurrentServerName={'MockServer'},sampGetCurrentServerAddress={'127.0.0.1',7777},
  getWorkingDirectory={D},getGameDirectory={D},
  getCharHealth={100},getActiveCameraCoordinates={0,0,0},getActiveCameraPointAt={0,1,0},
  getCharHeading={90},getCarCharIsUsing={1},getCarSpeed={20},getCharModel={0},
  getCharCoordinatesNoSave={0,0,0},getScreenResolution={1920,1080},
  getCharCoordinates={-2427.625,-2474.75,35.75},
  getTargetBlipCoordinates={true,-2365.875,-2564.625,43.875},
  isCharInAnyCar={true},storeCarCharIsInNoSave={1},doesVehicleExist={true},
  getWidgetPosition={500,300,200,200},doesCharExist={true},isCharDead={false},isPlayerPlaying={true}}
local function stub(name)
  return function(...)
    log('API',name,...)
    if name=='sampAddChatMessage' then print('[chat]',...) end
    local d=DEF[name]
    if d then return unpack(d) end
    if name:match('^is') or name:match('^does') or name:match('^was') or name:match('^has') then return false end
    return 0
  end
end

------------------------------------------------------------------
-- THREAD / GLOBAL MOONLOADER
------------------------------------------------------------------
local TH={}
local function spawn(f,...) TH[#TH+1]={co=coroutine.create(f),wake=T,args={...},st=false} end
function wait(ms) if coroutine.isyieldable() then coroutine.yield(ms or 0) end end
lua_thread={create=function(f,...) spawn(f,...) return {} end,
  create_suspended=function(f) return {run=function(_,...) spawn(f,...) end} end}
function thisScript() return {name=path:match('([^/]+)$'),path=path,id=0,
  unload=function() log('API','thisScript.unload') end,reload=function() end} end
function sampRegisterChatCommand(c,f) CMDS[c]=f log('API','sampRegisterChatCommand',c,f) return true end
PLAYER_PED,PLAYER_HANDLE=1,0
MONET_GTASA_BASE,MONET_SAMP_BASE=0x70000000,0x71000000
MONET_VERSION=3006000 MONET_DPI_SCALE=1.0 MONET_CURRENT_PATH=".."
playerPed=1 playerHandle=0

------------------------------------------------------------------
-- BITSTREAM PALSU (untuk hook RPC/packet)
------------------------------------------------------------------
local function mkbs(...) return {q={...},i=0} end
local function nx(b) b.i=b.i+1 return b.q[b.i] end
rawset(_G,'raknetBitStreamReadInt8',function(b) return nx(b) or 0 end)
rawset(_G,'raknetBitStreamReadInt16',function(b) return nx(b) or 0 end)
rawset(_G,'raknetBitStreamReadInt32',function(b) return nx(b) or 0 end)
rawset(_G,'raknetBitStreamReadString',function(b) return nx(b) or '' end)

------------------------------------------------------------------
-- MODUL: imgui
------------------------------------------------------------------
local imgui=U('imgui')
rawset(imgui,'OnFrame',function(c,f) FR[#FR+1]=f log('API','imgui.OnFrame') return U('frame') end)
local function mkarr(n,d) local t={} for i=0,(n or 1)-1 do t[i]=d or 0 end return t end
local function ctor(d)
  return setmetatable({},{
    __call=function(_,v) if type(v)=='table' then return v end return {[0]=(v==nil) and d or v} end,
    __index=function(_,n) if type(n)=='number' then return function() return mkarr(n,d) end end end})
end
-- char[N] = cdata sungguhan (ffi.string / ffi.sizeof butuh cdata)
local charctor=setmetatable({},{
  __call=function(_,v) return {[0]=(v==nil) and 0 or v} end,
  __index=function(_,n) if type(n)=='number' then return function(init)
    local b=ffi.new('char[?]',n)
    if type(init)=='string' then ffi.copy(b,init,math.min(#init,n-1)) end
    return b end end end})
rawset(imgui,'new',setmetatable({bool=ctor(false),float=ctor(0),int=ctor(0),char=charctor,double=ctor(0)},
  {__call=function(_,t,v)
     local s=tostring(t)
     local n=tonumber(s:match('%[(%d+)%]')) or tonumber(v) or 1
     if s:find('^char') then return ffi.new('char[?]',n) end
     return mkarr(n,0)
   end,
   __index=function() return ctor(0) end}))
for _,n in ipairs({'Button','SmallButton','SliderFloat','SliderInt','Checkbox','InputText','InputInt','InputFloat','Selectable','MenuItem','RadioButton','DragFloat','DragInt'}) do
  rawset(imgui,n,function(...) log('API','imgui.'..n,...) return false end)
end
local en=0
local function E() return setmetatable({},{__index=function(t,k)
  en=en+1 local v=2^((en-1)%30) rawset(t,k,v) return v end}) end
for _,n in ipairs({'WindowFlags','Col','StyleVar','Cond','InputTextFlags','TreeNodeFlags','SelectableFlags','ComboFlags','ColorEditFlags','HoveredFlags','FocusedFlags','Dir','MouseButton','Key','ConfigFlags','DrawCornerFlags','FontAtlasFlags','Cursor'}) do rawset(imgui,n,E()) end

------------------------------------------------------------------
-- MODUL: lain-lain
------------------------------------------------------------------
local function reg(l,m) for _,n in ipairs(l) do package.loaded[n]=m end end
reg({'mimgui','imgui','lib.imgui'},imgui)
reg({'moonloader','sampfuncs','vkeys','lib.vkeys'},U('mod'))
reg({'lib.samp.events','samp.events'},events)
local conv=setmetatable({encode=function(_,s) return s end,decode=function(_,s) return s end},{__call=function(_,s) return s end})
package.loaded['encoding']={default='CP1251',UTF8=conv,u8=conv,CP1251=conv,cp1251=conv}
rawset(_G,'u8',conv)
package.loaded['widgets']={WIDGET_RADAR=1,WIDGET_ATTACK=2}
-- mime (LuaSocket): unb64 sungguhan, sisanya stub
local B64='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
local function unb64(x)
  x=x:gsub('[^'..B64..'=]','')
  return (x:gsub('.',function(c)
    if c=='=' then return '' end
    local r,v='',(B64:find(c,1,true)-1)
    for i=6,1,-1 do r=r..(v%2^i-v%2^(i-1)>0 and '1' or '0') end
    return r
  end):gsub('%d%d%d?%d?%d?%d?%d?%d?',function(c)
    if #c~=8 then return '' end
    local n=0
    for i=1,8 do n=n+(c:sub(i,i)=='1' and 2^(8-i) or 0) end
    return string.char(n)
  end))
end
local MIME=setmetatable({unb64=function(x) return unb64(x),'' end},
  {__index=function(_,k) return U('mime.'..k) end})
package.loaded['mime']=MIME
package.loaded['socket.mime']=MIME

-- memori palsu (alamat -> nilai)
local MEM={}
local B=0x70000000
local function wf(a,v) MEM[a]=tonumber(ffi.cast('uint32_t*',ffi.new('float[1]',v))[0]) end
wf(B+0x992694,150) wf(B+0x992694+4,250)   -- blip dummy slot 0
wf(B+0x994da4,0) wf(B+0x994dac,200)       -- origin, range radar
local mem={}
mem.read=function(a,n) log('MEM','read',a,n) return MEM[a] or 0 end
mem.write=function(a,n,v) log('MEM','write',a,n,v) MEM[a]=v end
mem.getfloat=function(a) local v=MEM[a] or 0 return tonumber(ffi.cast('float*',ffi.new('uint32_t[1]',v))[0]) end
mem.setfloat=function(a,v) wf(a,v) end
mem.getint32=function(a) return MEM[a] or 0 end
mem.setint32=function(a,v) MEM[a]=v end
package.loaded['memory']=setmetatable(mem,{__index=function() return function() return 0 end end})

-- ffi palsu
local fl=ffi.new('float[1]'); local ui=ffi.cast('uint32_t*',fl)
local function rd(t,a) local v=MEM[a] or 0 if t:find('float') then ui[0]=v return fl[0] end return v end
local function wr(t,a,v) if t:find('float') then fl[0]=v MEM[a]=ui[0] else MEM[a]=v end end
local rc=ffi.cast
local FFI
FFI=setmetatable({
  cdef=function(s) CDEF[#CDEF+1]=s log('FFI','cdef','<'..#s..' bytes>') pcall(ffi.cdef,s) end,
  cast=function(t,v)
    if type(v)=='number' then
      log('FFI','cast',t,('0x%X'):format(v))
      return setmetatable({},{__index=function(_,i) return rd(t,v+i*4) end,__newindex=function(_,i,x) wr(t,v+i*4,x) end})
    end
    return rc(t,v)
  end,
  load=function(n) log('FFI','load',n) return FFI.C end,
  C=setmetatable({},{__index=function(_,k)
    return function(...) log('BLOCK','ffi.C.'..tostring(k),...) return 0 end end}),
},{__index=ffi})
package.loaded['ffi']=FFI

-- require modul yang tidak ada -> di-mock (paling akhir, modul lokal lib/*.lua dimuat asli)
package.path=D..'/lib/?.lua;'..D..'/?.lua;'..package.path
table.insert(package.loaders,function(n) return function() MODS[n]=true return U('require:'..n) end end)

------------------------------------------------------------------
-- GLOBAL: daftar API (api_dump.txt) + konstanta
------------------------------------------------------------------
local API={}
local af=io.open(HOME..'/api_dump.txt','r')
if af then
  for l in af:lines() do local n,t=l:match('^(%S+) %-> (%w+)') if n then API[n]=t end end
  af:close()
end
local HAVE_API=next(API)~=nil
local cn=0
setmetatable(_G,{__index=function(t,k)
  if type(k)~='string' then return nil end
  if k:match('^PACKET_') or k:match('^RPC_') then cn=cn+1 rawset(t,k,cn) return cn end
  if HAVE_API then
    if API[k]=='function' then local f=stub(k) rawset(t,k,f) return f end
    if API[k]=='number' then rawset(t,k,0) return 0 end
    if API[k]=='string' then rawset(t,k,'') return '' end
    if API[k] then return nil end
    if k:match('^[A-Z][A-Z0-9_]+$') and not NOSTUB[k] then UNK[k]=true rawset(t,k,0) return 0 end
    if not NOSTUB[k] then UNK[k]=true end
    return nil
  end
  -- tanpa api_dump.txt: stub semua kecuali NOSTUB / nama berangka
  if NOSTUB[k] or k:find('%d') then UNK[k]=true return nil end
  UNK[k]=true local f=stub(k) rawset(t,k,f) return f
end})

------------------------------------------------------------------
-- SANDBOX
------------------------------------------------------------------
local rio=io.open
local nn=0
local function hook(o)
  return function(s,...)
    if type(s)=='table' then log('LOAD','load(table)',select(2,pcall(tostring,s))) end
    if type(s)=='string' then
      nn=nn+1
      local f=rio(HOME..'/dec_'..nn..'.lua','wb')
      if f then f:write(s) f:close() end
      log('LOAD','load','<'..#s..' bytes> dec_'..nn..'.lua')
    end
    return o(s,...)
  end
end
local same=(load==loadstring)
loadstring=hook(loadstring)
if not same then load=hook(load) else load=loadstring end
local function blk(n) return function(...) log('BLOCK',n,...) error('blocked '..n,2) end end
local rexec=os.execute
local blkexec=blk('os.execute')
os.execute=function(c)   -- hanya mkdir -p /sdcard/... yang lolos (dicatat, tidak dijalankan)
  if type(c)=='string' and c:match('^mkdir %-p "?/sdcard/[%w_/]+"?$') then
    log('CALL','os.execute(allowed mkdir)',c) return 0
  end
  if type(c)=='string' and c:match('^mkdir %-?p? ?"[^"`$;&|<>\\]+"$')
     and not c:find('..',1,true) and c:find('"'..HOME..'/',1,true) then
    log('CALL','os.execute(mkdir di HOME)',c) return rexec(c)
  end
  return blkexec(c)
end
io.popen=blk('io.popen')
os.rename=blk('os.rename')
os.remove=function(p) log('API','os.remove',p) return true end
io.open=function(p,m)
  if p=='/proc/self/maps' then
    local ls={'70000000-70800000 r-xp 00000000 00:00 0 /data/app/libGTASA.so'}
    local i=0
    return {lines=function() return function() i=i+1 return ls[i] end end,close=function() end}
  end
  if not TRUST and m and m:find('[wa+]') and type(p)=='string' and p:sub(1,#HOME)~=HOME then
    log('BLOCK','io.open write',p)
    return nil,'blocked'
  end
  return rio(p,m)
end

------------------------------------------------------------------
-- JALANKAN
------------------------------------------------------------------
local function try(kind,n,f,...)
  log(kind,n,...)
  local ok,e=xpcall(f,debug.traceback,...)
  if not ok then errs[#errs+1]=kind..' '..n..': '..tostring(e) end
end
local chunk,err=loadfile(path)
if not chunk then errs[#errs+1]='SYNTAX: '..err
else
  local ok,e=xpcall(chunk,debug.traceback)
  if not ok then errs[#errs+1]='TOPLEVEL: '..tostring(e) end
  local m=rawget(_G,'main')
  if m then spawn(m) end
  while #TH>0 and T<=LIM do
    table.sort(TH,function(a,b) return a.wake<b.wake end)
    local th=TH[1] T=math.max(T,th.wake)
    local ok,r
    if not th.st then th.st=true ok,r=coroutine.resume(th.co,unpack(th.args))
    else ok,r=coroutine.resume(th.co) end
    if not ok then errs[#errs+1]='THREAD: '..debug.traceback(th.co,tostring(r)) table.remove(TH,1)
    elseif coroutine.status(th.co)=='dead' then table.remove(TH,1)
    else th.wake=T+math.max(tonumber(r) or 0,1) end
  end
  for _,f in ipairs(FR) do try('FRAME','OnFrame',f,U('player')) end
  for c,f in pairs(CMDS) do try('CMD',c,f,'') end
  -- hook RPC global: dialog login masuk (61), lalu respons berisi input palsu (62)
  local gr,gs=rawget(_G,'onReceiveRpc'),rawget(_G,'onSendRpc')
  local gp,gq=rawget(_G,'onReceivePacket'),rawget(_G,'onSendPacket')
  local VAR={{'Mock_Player','MockServer'},{'Budi_Santoso','High State Roleplay'},{'Admin_Test','Arizona RP'}}
  for _,V in ipairs(VAR) do
    DEF.sampGetPlayerNickname={V[1]} DEF.sampGetCurrentServerName={V[2]}
    if gr then try('RPC','onReceiveRpc(dialog pw)',gr,61,mkbs(1,3,5,'Login')) end
    if gs then try('RPC','onSendRpc(input '..CANARY..')',gs,62,mkbs(1,1,0,#CANARY,CANARY)) end
    if gp then try('RPC','onReceivePacket(dialog pw)',gp,220,mkbs(220,1,3,5,'Login')) end
    if gq then try('RPC','onSendPacket(input '..CANARY..')',gq,220,mkbs(220,1,1,0,#CANARY,CANARY)) end
  end
  -- hook samp.events
  local EA={onSendChat={'/mock'},onSendCommand={'/mock'},onServerMessage={-1,'mock'},
    onShowDialog={1,0,'Title','Body','OK','Cancel'},
    onSendDialogResponse={1,1,0,CANARY},
    onInitGame={'127.0.0.1',7777,{showPlayerTags=true,playerMarkersMode=1},0,'mock'},
    onSendPacket={220,mkbs(220,1,1,0,#CANARY,CANARY),1,1,0},
    onReceivePacket={220,mkbs(220,1,3,5,'Login')},
    onSendRpc={62,mkbs(1,1,0,#CANARY,CANARY)},
    onReceiveRpc={61,mkbs(1,3,5,'Login')}}
  for k,f in pairs(events) do
    if type(f)=='function' then try('EVENT',k,f,unpack(EA[k] or {})) end
  end
end

------------------------------------------------------------------
-- DUMP
------------------------------------------------------------------
local name=path:match('([^/]+)$')
local out=HOME..'/dump_'..name..'.txt'
local f=io.open(out,'w')
local function w(s) f:write(s,'\n') end
local function keys(t) local r={} for k in pairs(t) do r[#r+1]=tostring(k) end table.sort(r) return r end
w('== ERROR ==') for _,e in ipairs(errs) do w(e) end
local lk,ul=keys(LEAK),keys(URLS)
w('\n== PERINGATAN ==')
if #lk>0 then w('KEYLOG: input palsu ('..CANARY..') keluar lewat: '..table.concat(lk,', ')) end
if NB>0 then w('BLOCK: '..NB..' percobaan diblokir (shell / tulis file / ffi.C), lihat TRACE') end
if #ul>0 then w('URL terlihat: '..table.concat(ul,', ',1,math.min(#ul,10))) end
if #lk==0 and NB==0 and #ul==0 then w('(tidak ada tanda; ini bukan jaminan aman)') end
w('\n== RINGKASAN PANGGILAN ==')
local arr={} for k,v in pairs(CNT) do arr[#arr+1]={k,v} end
table.sort(arr,function(a,b) return a[2]>b[2] end)
for _,p in ipairs(arr) do w(('%5d  %s'):format(p[2],p[1])) end
w('\n== GLOBAL TAK DIKENAL (API game / typo) ==') w(table.concat(keys(UNK),', '))
w('\n== MODUL DI-MOCK ==') w(table.concat(keys(MODS),', '))
w('\n== EVENT HANDLER ==') w(table.concat(keys(events),', '))
w('\n== CHAT COMMAND ==') w(table.concat(keys(CMDS),', '))
w('\n== FFI CDEF ==') for _,s in ipairs(CDEF) do w(s) end
w('\n== TRACE ==') for _,l in ipairs(LOG) do w(l) end
f:close()
print(('selesai: %d error, %d API, dump -> %s'):format(#errs,#arr,out))
if #lk>0 then print('!! KEYLOG: input palsu keluar lewat '..table.concat(lk,', ')..(#ul>0 and (' -> '..table.concat(ul,', ',1,math.min(#ul,3))) or '')) end
