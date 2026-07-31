/* ============================================================
   Saathi — a simple voice helper for elders
   Pure browser app: no server, no keys. Uses the phone's own
   speech recognition + text-to-speech (Web Speech API).
   ============================================================ */

'use strict';

/* ---------- Tiny helpers ---------- */
const $  = (s, r=document) => r.querySelector(s);
const $$ = (s, r=document) => [...r.querySelectorAll(s)];
const pad = n => String(n).padStart(2,'0');
const nowHM = () => { const d=new Date(); return pad(d.getHours())+':'+pad(d.getMinutes()); };
const todayKey = () => new Date().toISOString().slice(0,10);
const uid = () => Math.random().toString(36).slice(2,9);

/* ---------- Persistent store ---------- */
const DB = {
  load(k, d){ try{ return JSON.parse(localStorage.getItem('saathi_'+k)) ?? d; }catch{ return d; } },
  save(k, v){ localStorage.setItem('saathi_'+k, JSON.stringify(v)); },
};
let state = {
  medicines : DB.load('medicines', []),   // {id,name,times:[],taken:{date,doneTimes:[]}}
  contacts  : DB.load('contacts',  []),    // {id,name,phone}
  settings  : DB.load('settings', { lang:'en-US', rate:0.85, name:'', groqKey:'', wake:false }),
};
// Pick up an optional key from a git-ignored config.js (window.LIFEOS_KEY)
if(!state.settings.groqKey && window.LIFEOS_KEY) state.settings.groqKey = window.LIFEOS_KEY;
const persist = () => {
  DB.save('medicines', state.medicines);
  DB.save('contacts',  state.contacts);
  DB.save('settings',  state.settings);
};

/* ---------- Languages offered (BCP-47) ---------- */
const LANGS = [
  ['en-US','English'], ['hi-IN','हिन्दी Hindi'], ['bn-IN','বাংলা Bengali'],
  ['ta-IN','தமிழ் Tamil'], ['te-IN','తెలుగు Telugu'], ['mr-IN','मराठी Marathi'],
  ['gu-IN','ગુજરાતી Gujarati'], ['kn-IN','ಕನ್ನಡ Kannada'], ['ml-IN','മലയാളം Malayalam'],
  ['pa-IN','ਪੰਜਾਬੀ Punjabi'], ['ur-PK','اردو Urdu'], ['ar-SA','العربية Arabic'],
  ['es-ES','Español Spanish'], ['fr-FR','Français French'], ['de-DE','Deutsch German'],
  ['pt-BR','Português'], ['ru-RU','Русский Russian'], ['zh-CN','中文 Chinese'],
  ['ja-JP','日本語 Japanese'], ['id-ID','Indonesian'], ['fil-PH','Filipino'],
];

/* ============================================================
   VOICE — speak (slow + friendly) and listen (any language)
   ============================================================ */
const Voice = {
  synth: window.speechSynthesis,
  voices: [],
  refresh(){ this.voices = this.synth ? this.synth.getVoices() : []; },

  pickVoice(lang){
    const base = lang.split('-')[0];
    const vs = this.voices;
    // prefer natural / neural voices — they sound much less robotic
    const nice = v => /google|natural|neural|premium|enhanced|siri|aria|jenny/i.test(v.name);
    return vs.find(v=>v.lang===lang && nice(v))
        || vs.find(v=>v.lang && v.lang.startsWith(base) && nice(v))
        || vs.find(v=>v.lang===lang)
        || vs.find(v=>v.lang && v.lang.startsWith(base))
        || vs.find(v=>nice(v))
        || null;
  },

  speak(text, opts={}){
    if(!this.synth) return;
    this.synth.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang  = opts.lang || state.settings.lang;
    u.rate  = opts.rate ?? state.settings.rate;   // slow by default
    u.pitch = 1.0;                                 // natural, gentle
    u.volume= 1;
    const v = this.pickVoice(u.lang);
    if(v) u.voice = v;
    this.synth.speak(u);
    lastSpoken = text;
  },

  /* one-shot listen; returns transcript via callback */
  listen(onResult, onEnd){
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if(!SR){ toast("Sorry, this phone/browser can't listen. Use the big buttons."); onEnd&&onEnd(); return; }
    const rec = new SR();
    rec.lang = state.settings.lang;
    rec.interimResults = false;
    rec.maxAlternatives = 3;
    rec.onresult = e => {
      const alts = [...e.results[0]].map(r=>r.transcript);
      onResult(alts[0], alts);
    };
    rec.onerror = () => {};
    rec.onend   = () => onEnd && onEnd();
    try{ rec.start(); }catch{ onEnd&&onEnd(); }
    return rec;
  }
};
let lastSpoken = '';
if(Voice.synth){ Voice.refresh(); Voice.synth.onvoiceschanged = ()=>Voice.refresh(); }

/* ============================================================
   COMMAND UNDERSTANDING (keyword based, multi-language)
   Recognition already transcribes the spoken language; here we
   match common command words across many languages, then find
   the contact name inside the sentence.
   ============================================================ */
const KW = {
  call:    ['call','phone','dial','ring','कॉल','फोन','फ़ोन','call karo','बुलाओ','फون','அழை','కాల్','ಕರೆ','ফোন','llama','llamar','appelle','anruf','ligar','позвони','打电话','電話','telepon','tawagan'],
  whatsapp:['whatsapp','व्हाट्सएप','वॉट्सऐप','whats app','వాట్సాప్','வாட்ஸ்அப்'],
  message: ['message','text','sms','sandesh','संदेश','मैसेज','मेसेज','மெசேஜ்','mensaje','nachricht','messaggio'],
  torchOn: ['torch','light','flashlight','flash','lamp','टॉर्च','रोशनी','लाइट','बत्ती','விளக்கு','लैंप','linterna','lampe','taschenlampe','手电','灯'],
  torchOff:['off','band','बंद','बुझा','ऑफ','close','apaga','aus','关'],
  youtube: ['youtube','you tube','video','यूट्यूब','वीडियो','गाना','song','music','गीत','भजन','வீடியோ','videos'],
  medTaken:['taken','took','done','khaya','खा लिया','ले लिया','खाई','हो गया','finished','खा ली','सेवन','tomé','genommen'],
  time:    ['time','kya time','samay','समय','टाइम','बजे','hora','zeit','时间','clock'],
  help:    ['help','madad','मदद','सहायता','bachao','बचाओ','ayuda','hilfe','emergency','救命'],
};
const hasKW = (t, list) => list.some(w => t.includes(w));

function normalize(s){ return ' '+s.toLowerCase().replace(/[.,!?]/g,' ').replace(/\s+/g,' ')+' '; }

/* find a saved contact mentioned in the sentence */
function findContact(t){
  let best=null, bestLen=0;
  for(const c of state.contacts){
    const name = c.name.toLowerCase();
    if(name && t.includes(name) && name.length>bestLen){ best=c; bestLen=name.length; }
  }
  return best;
}

/* ============================================================
   WHISPER — record audio and transcribe in ANY language (Groq)
   Far better than the browser recognizer, especially for Hindi.
   ============================================================ */
const Rec = {
  mr:null, chunks:[], stream:null,
  supported(){ return !!(navigator.mediaDevices && window.MediaRecorder); },
  async start(){
    this.stream = await navigator.mediaDevices.getUserMedia({audio:true});
    this.chunks = [];
    const mime = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : '';
    this.mr = mime ? new MediaRecorder(this.stream,{mimeType:mime}) : new MediaRecorder(this.stream);
    this.mr.ondataavailable = e => { if(e.data && e.data.size) this.chunks.push(e.data); };
    this.mr.start();
  },
  stop(){
    return new Promise(res=>{
      if(!this.mr){ res(null); return; }
      this.mr.onstop = ()=>{
        const blob = new Blob(this.chunks, {type:this.mr.mimeType||'audio/webm'});
        this.stream.getTracks().forEach(t=>t.stop());
        this.mr = null;
        res(blob);
      };
      try{ this.mr.stop(); }catch{ res(null); }
    });
  }
};

async function whisperTranscribe(blob){
  const key = state.settings.groqKey;
  if(!key || !blob) return null;
  const fd = new FormData();
  fd.append('model','whisper-large-v3');
  fd.append('response_format','json');
  const lg = (state.settings.lang||'').split('-')[0];
  if(lg && lg!=='en') fd.append('language', lg);   // hint helps Hindi etc.
  fd.append('file', blob, 'voice.webm');
  try{
    const r = await fetch('https://api.groq.com/openai/v1/audio/transcriptions', {
      method:'POST', headers:{ 'Authorization':'Bearer '+key }, body:fd
    });
    if(!r.ok) return null;
    const j = await r.json();
    return (j.text||'').trim();
  }catch{ return null; }
}

/* ---------- AI brain (Groq Llama) — understands anything, answers questions ---------- */
async function aiUnderstand(raw){
  const key = state.settings.groqKey;
  if(!key) return null;
  const names = state.contacts.map(c=>c.name).join(', ') || '(none saved yet)';
  const sys =
    "You are LifeOS, a kind voice helper for an elderly person. The user may speak ANY language. "
    + "Decide ONE action and a spoken reply. Known people you can contact: " + names + ". "
    + "Reply ONLY as a JSON object with keys: action, person, query, message, reply. "
    + "action is one of: call, whatsapp, message, torch_on, torch_off, youtube, time, medicine_taken, answer, none. "
    + "If the user asks ANY question or wants to chat (maths, facts, general knowledge, jokes, advice), "
    + "use action \"answer\" and put the full, correct, helpful answer in reply. "
    + "person = exact name from the known people list, or empty. query = search text for youtube. "
    + "message = the message text (for whatsapp/message). "
    + "reply = a short, warm sentence spoken in the SAME language the user used. Keep it simple and clear for an elder.";
  try{
    const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method:'POST',
      headers:{ 'Authorization':'Bearer '+key, 'Content-Type':'application/json' },
      body: JSON.stringify({
        model:'llama-3.3-70b-versatile', temperature:0.2,
        response_format:{ type:'json_object' },
        messages:[ {role:'system',content:sys}, {role:'user',content:raw} ]
      })
    });
    if(!r.ok) return null;
    const j = await r.json();
    return JSON.parse(j.choices[0].message.content);
  }catch{ return null; }
}

function findContactByName(name){
  if(!name) return null;
  const p = name.toLowerCase();
  return state.contacts.find(c=>c.name.toLowerCase()===p)
      || state.contacts.find(c=>p.includes(c.name.toLowerCase()) || c.name.toLowerCase().includes(p))
      || null;
}

function runIntent(i){
  const c = findContactByName(i.person);
  const r = i.reply;
  switch((i.action||'').toLowerCase()){
    case 'call': if(c){ speakAndShow(r||`Calling ${c.name}.`); openCall(c); } else { speakAndShow(r||"Who should I call? Please add them."); renderContacts(); go('call'); } break;
    case 'whatsapp': if(c){ speakAndShow(r||`Opening WhatsApp for ${c.name}.`); openWhatsApp(c); } else { speakAndShow(r||"Please add the person first."); renderContacts(); go('call'); } break;
    case 'message': if(c){ speakAndShow(r||`Message to ${c.name}.`); openSMS(c); } else { speakAndShow(r||"Please add the person first."); renderContacts(); go('call'); } break;
    case 'torch_on': Torch.set(true); speakAndShow(r||"Torch on."); break;
    case 'torch_off': Torch.set(false); speakAndShow(r||"Torch off."); break;
    case 'youtube': speakAndShow(r||"Opening YouTube."); openYouTube(i.query||''); break;
    case 'time': { const d=new Date(); speakAndShow(r||`It is ${(d.getHours()%12)||12}:${pad(d.getMinutes())} ${d.getHours()<12?'in the morning':'in the evening'}.`); break; }
    case 'medicine_taken': { const n=markAnyDueTaken(); speakAndShow(r||(n?"Well done. I marked your medicine as taken.":"No medicine is due right now.")); break; }
    case 'answer': default: speakAndShow(r||"Okay."); break;
  }
}

/* Main entry: try the AI brain first (if a key is set), else keyword matching. */
async function handleCommand(raw){
  if(state.settings.groqKey){
    setThinking(true);
    const intent = await aiUnderstand(raw);
    if(intent && intent.action){ runIntent(intent); return; }
    // AI failed (offline / bad key) — fall back to simple commands
  }
  handleCommandLocal(raw);
}

/* ---------- Offline keyword matching (works with no key) ---------- */
function handleCommandLocal(raw){
  const t = normalize(raw);

  if(hasKW(t, KW.help)){
    speakAndShow(say('help_intro'));
    go('help'); return;
  }
  if(hasKW(t, KW.time)){
    const d=new Date();
    speakAndShow(`It is ${pad(d.getHours()%12||12)}:${pad(d.getMinutes())} ${d.getHours()<12?'in the morning':'in the evening'}.`);
    return;
  }
  if(hasKW(t, KW.medTaken)){
    const n = markAnyDueTaken();
    speakAndShow(n ? "Well done. I marked your medicine as taken. ✅" : "Okay. I don't see a medicine due right now.");
    return;
  }
  if(hasKW(t, KW.torchOff) && !hasKW(t, KW.call)){ Torch.set(false); speakAndShow("Torch off."); return; }
  if(hasKW(t, KW.torchOn)){ Torch.toggle(); speakAndShow("Here is the torch."); return; }

  if(hasKW(t, KW.youtube)){
    const q = raw.replace(/you\s*tube|youtube|यूट्यूब/gi,'').replace(/\b(open|play|search|find|चलाओ|खोलो)\b/gi,'').trim();
    speakAndShow(q ? `Opening YouTube for ${q}.` : "Opening YouTube.");
    openYouTube(q); return;
  }

  const wantsCall = hasKW(t, KW.call);
  const wantsWA   = hasKW(t, KW.whatsapp);
  const wantsMsg  = hasKW(t, KW.message);
  if(wantsCall || wantsWA || wantsMsg){
    const c = findContact(t);
    if(!c){ speakAndShow("Who should I contact? Please tap Call Someone and add them first."); go('call'); return; }
    if(wantsWA){ speakAndShow(`Opening WhatsApp for ${c.name}.`); openWhatsApp(c); return; }
    if(wantsMsg){ speakAndShow(`Message to ${c.name}.`); openSMS(c); return; }
    speakAndShow(`Calling ${c.name}. Please tap the green call button.`); openCall(c); return;
  }

  // fallback
  speakAndShow(state.settings.groqKey
    ? "Sorry, I didn't understand. Please try again."
    : "I can do call, torch, YouTube, and medicine. To answer questions like maths, add the free AI key in Setup ⚙️.");
}

/* ============================================================
   FRIENDLY PHRASES  (kept very simple)
   ============================================================ */
function say(id){
  const name = state.settings.name ? (', '+state.settings.name) : '';
  const m = {
    greet: `Hello${name}. I am here to help you.`,
    listening: "I am listening.",
    help_intro: "I can help you. You can tap the big buttons, or tap the green microphone and just talk. Say things like: call, torch, or YouTube.",
    med_due: (n)=>`It is time for your medicine${name}. Please take ${n}.`,
    med_again: (n)=>`Please do not forget your medicine, ${n}.`,
    med_ok: "Very good. Take your time.",
  };
  return m[id];
}

/* ============================================================
   MEDICINE REMINDERS — nag until verified
   ============================================================ */
const Meds = {
  isTakenNow(m, time){
    return m.taken && m.taken.date===todayKey() && m.taken.doneTimes.includes(time);
  },
  markTaken(m, time){
    if(!m.taken || m.taken.date!==todayKey()) m.taken={date:todayKey(), doneTimes:[]};
    if(!m.taken.doneTimes.includes(time)) m.taken.doneTimes.push(time);
    persist(); renderMeds();
  },
  /* returns list of {med,time} due now and not taken */
  dueNow(){
    const cur = nowHM(); const out=[];
    for(const m of state.medicines){
      for(const time of (m.times||[])){
        if(this.isTakenNow(m,time)) continue;
        // due if scheduled time has passed today (within same day) and not taken
        if(cur >= time) out.push({med:m, time});
      }
    }
    return out;
  }
};

let activeReminder = null;   // {med,time}
let nagTimer = null;

function checkReminders(){
  if(activeReminder) return;                 // already showing one
  const due = Meds.dueNow();
  if(due.length){ showReminder(due[0]); }
}
function showReminder(item){
  activeReminder = item;
  $('#remText').textContent = `Time for your medicine:\n${item.med.name}`;
  $('#reminder').classList.add('show');
  Voice.speak(say('med_due')(item.med.name));
  buzz();
  notify('💊 Medicine time', `Please take ${item.med.name}`);
  clearInterval(nagTimer);
  // keep nagging every 60s until verified
  nagTimer = setInterval(()=>{
    if(!activeReminder){ clearInterval(nagTimer); return; }
    Voice.speak(say('med_again')(activeReminder.med.name));
    buzz(); notify('💊 Please take your medicine', activeReminder.med.name);
  }, 60000);
}
function verifyTaken(){
  if(!activeReminder) return;
  Meds.markTaken(activeReminder.med, activeReminder.time);
  Voice.speak(say('med_ok'));
  closeReminder();
}
function snoozeReminder(){
  const item = activeReminder; closeReminder();
  setTimeout(()=>{ if(!activeReminder) showReminder(item); }, 5*60*1000);
}
function closeReminder(){
  clearInterval(nagTimer); nagTimer=null; activeReminder=null;
  $('#reminder').classList.remove('show');
}
/* voice "I took it" when no overlay -> mark earliest due */
function markAnyDueTaken(){
  if(activeReminder){ verifyTaken(); return true; }
  const due = Meds.dueNow();
  if(due.length){ Meds.markTaken(due[0].med, due[0].time); return true; }
  return false;
}

/* ============================================================
   DEVICE ACTIONS
   ============================================================ */
const Torch = {
  track:null, on:false,
  async toggle(){ this.on ? this.set(false) : this.set(true); },
  async set(want){
    try{
      if(want){
        if(!this.track){
          const s = await navigator.mediaDevices.getUserMedia({video:{facingMode:'environment'}});
          this.track = s.getVideoTracks()[0];
        }
        await this.track.applyConstraints({advanced:[{torch:true}]});
        this.on=true;
      }else if(this.track){
        await this.track.applyConstraints({advanced:[{torch:false}]});
        this.track.stop(); this.track=null; this.on=false;
      }
    }catch(e){
      toast("Torch not available on this phone. Use the phone's own torch.");
    }
  }
};
const digits = p => (p||'').replace(/[^\d+]/g,'');
function openCall(c){ location.href = 'tel:'+digits(c.phone); }
function openSMS(c){  location.href = 'sms:'+digits(c.phone); }
function openWhatsApp(c){ window.open('https://wa.me/'+digits(c.phone).replace(/^\+/,''), '_blank'); }
function openYouTube(q){
  const url = q ? 'https://www.youtube.com/results?search_query='+encodeURIComponent(q)
                : 'https://www.youtube.com/';
  window.open(url, '_blank');
}

/* ============================================================
   UI — navigation & rendering
   ============================================================ */
function go(id){
  $$('.screen').forEach(s=>s.classList.toggle('active', s.id===id));
  window.scrollTo(0,0);
}
/* ---------- Conversation view (chat-like, like a real assistant) ---------- */
function addMsg(role, text){
  const c = $('#convo'); if(!c) return null;
  const empty = $('#emptyState'); if(empty) empty.remove();
  const b = document.createElement('div');
  b.className = 'msg ' + role;
  b.textContent = text;
  c.appendChild(b); c.scrollTop = c.scrollHeight;
  return b;
}
let typingEl = null;
function setThinking(on){
  if(on){ if(!typingEl){ typingEl = addMsg('lifeos typing', '•••'); } }
  else if(typingEl){ typingEl.remove(); typingEl = null; }
}
function setStatus(t){ const s=$('#talkLabel'); if(s) s.textContent = t; }

function speakAndShow(text){ setThinking(false); addMsg('lifeos', text); Voice.speak(text); }

function renderMeds(){
  const box = $('#medList'); box.innerHTML='';
  if(!state.medicines.length){ box.innerHTML = `<p class="hint">No medicines yet. Tap the button below to add one.</p>`; }
  for(const m of state.medicines){
    const done = (m.times||[]).every(t=>Meds.isTakenNow(m,t));
    const el = document.createElement('div');
    el.className = 'item'+(done?' taken':'');
    el.innerHTML = `
      <span class="emoji">💊</span>
      <div class="grow">
        <div class="big">${escapeHtml(m.name)}</div>
        <div class="sub">${(m.times||[]).map(t=>fmt12(t)).join('  •  ')||'no time set'}</div>
      </div>
      ${done?'<span class="badge">Taken ✓</span>':'<button class="pillbtn done">✅ Took it</button>'}
      <button class="pillbtn del" title="Remove">🗑️</button>`;
    if(!done) el.querySelector('.done').onclick = ()=>{
      (m.times||[]).forEach(t=>Meds.markTaken(m,t)); Voice.speak("Well done.");
    };
    el.querySelector('.del').onclick = ()=>{
      if(confirm('Remove '+m.name+'?')){ state.medicines = state.medicines.filter(x=>x.id!==m.id); persist(); renderMeds(); }
    };
    box.appendChild(el);
  }
}
function renderContacts(){
  const box = $('#contactList'); box.innerHTML='';
  if(!state.contacts.length){ box.innerHTML = `<p class="hint">No people yet. Add family so you can say "call beta".</p>`; }
  for(const c of state.contacts){
    const el = document.createElement('div');
    el.className='item';
    el.innerHTML = `
      <span class="emoji">🧑</span>
      <div class="grow"><div class="big">${escapeHtml(c.name)}</div><div class="sub">${escapeHtml(c.phone)}</div></div>
      <button class="pillbtn go">📞</button>
      <button class="pillbtn wa">🟢</button>
      <button class="pillbtn del">🗑️</button>`;
    el.querySelector('.go').onclick = ()=>openCall(c);
    el.querySelector('.wa').onclick = ()=>openWhatsApp(c);
    el.querySelector('.del').onclick = ()=>{ if(confirm('Remove '+c.name+'?')){ state.contacts=state.contacts.filter(x=>x.id!==c.id); persist(); renderContacts(); } };
    box.appendChild(el);
  }
}

/* ---------- simple form dialog builder ---------- */
function openForm(title, fields, onSave){
  $('#formTitle').textContent = title;
  const wrap = $('#formFields'); wrap.innerHTML='';
  const inputs = {};
  for(const f of fields){
    const l = document.createElement('label'); l.textContent=f.label;
    let input;
    if(f.type==='times'){                       // multiple times
      input = document.createElement('div');
      const chips = document.createElement('div'); chips.className='timerow';
      const add = document.createElement('input'); add.type='time';
      const btn = document.createElement('button'); btn.type='button'; btn.textContent='➕ Add time'; btn.className='testbtn';
      let times = f.value ? [...f.value] : [];
      const draw = ()=>{ chips.innerHTML=''; times.forEach((t,i)=>{ const c=document.createElement('span'); c.className='timechip'; c.innerHTML=fmt12(t)+' <button type="button">✕</button>'; c.querySelector('button').onclick=()=>{times.splice(i,1);draw();}; chips.appendChild(c); }); };
      btn.onclick=()=>{ if(add.value && !times.includes(add.value)){ times.push(add.value); times.sort(); draw(); } };
      draw();
      input.append(chips, add, btn);
      inputs[f.name]=()=>times;
    }else{
      input = document.createElement('input'); input.type=f.type||'text';
      input.value=f.value||''; if(f.placeholder) input.placeholder=f.placeholder;
      inputs[f.name]=()=>input.value.trim();
    }
    const g=document.createElement('div'); g.append(l,input); wrap.appendChild(g);
  }
  const dlg=$('#formDlg');
  $('#formEl').onsubmit = ()=>{
    if($('#formEl').returnValue!=='cancel'){} // handled by close
  };
  dlg.returnValue='';
  dlg.onclose = ()=>{ if(dlg.returnValue==='ok'){ const data={}; for(const k in inputs) data[k]=inputs[k](); onSave(data); } };
  dlg.showModal();
}

/* ============================================================
   MISC — clock, notifications, buzz, toast, escaping
   ============================================================ */
function fmt12(hm){ const [h,m]=hm.split(':').map(Number); const ap=h<12?'AM':'PM'; return `${(h%12)||12}:${pad(m)} ${ap}`; }
function escapeHtml(s){ return (s||'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
function buzz(){ try{ navigator.vibrate && navigator.vibrate([300,150,300]); }catch{} }
function toast(msg){ addMsg('lifeos', msg); }
function notify(title, body){
  if(!('Notification' in window)) return;
  if(Notification.permission==='granted'){ try{ new Notification(title,{body,icon:'icons/icon.svg'});}catch{} }
}
function tickClock(){ const el=$('#clock'); if(!el) return; const d=new Date(); el.textContent = `${(d.getHours()%12)||12}:${pad(d.getMinutes())} ${d.getHours()<12?'AM':'PM'}`; }

/* ============================================================
   WIRING
   ============================================================ */
/* ============================================================
   WAKE WORD — always listening for the name "LifeOS"
   Lightweight on-device recognition; when it hears the name it
   handles whatever was said after it. Hands-free.
   ============================================================ */
const Wake = {
  rec:null, on:false, paused:false,
  words:['lifeos','sarathi','saarti','sarthi','साथी','सारथी','साथि','ساتھی'],
  start(){
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if(!SR || this.rec) return;
    const rec = new SR();
    rec.lang = state.settings.lang; rec.continuous = true; rec.interimResults = false;
    rec.onresult = e => {
      for(let i=e.resultIndex; i<e.results.length; i++){
        if(e.results[i].isFinal) this.heard(e.results[i][0].transcript);
      }
    };
    rec.onerror = ()=>{};
    rec.onend = ()=>{ if(this.on && !this.paused){ try{ rec.start(); }catch{} } };
    this.rec = rec; this.on = true;
    try{ rec.start(); }catch{}
  },
  stop(){ this.on=false; if(this.rec){ try{ this.rec.stop(); }catch{} this.rec=null; } },
  heard(said){
    const s = ' '+said.toLowerCase().trim()+' ';
    const hit = this.words.find(w => s.includes(w));
    if(!hit) return;
    let cmd = said.toLowerCase().slice(s.indexOf(hit)-1 + hit.length).replace(/^[\s,।!?.-]+/,'').trim();
    if(cmd){ addMsg('you', said.trim()); handleCommand(cmd); }
    else { speakAndShow('Yes? I am listening.'); }
  }
};
function pauseWake(){ if(Wake.on){ Wake.paused=true; try{Wake.rec && Wake.rec.stop();}catch{} } }
function resumeWake(){ if(state.settings.wake){ Wake.paused=false; if(!Wake.rec) Wake.start(); else { try{Wake.rec.start();}catch{} } } }

/* ---------- Listening flow: Whisper (tap to start / tap to stop) or browser ---------- */
let listenBusy = false;
async function onTalk(){
  const btn = $('#talkBtn');
  if(btn.classList.contains('listening')){
    if(btn.dataset.mode === 'whisper') await finishWhisper();   // stop & transcribe
    return;
  }
  if(listenBusy) return;
  pauseWake();                                    // free the mic
  if(state.settings.groqKey && Rec.supported()){
    try{
      await Rec.start();
      btn.classList.add('listening'); btn.dataset.mode = 'whisper';
      setStatus('Listening… tap to stop');
    }catch(e){ browserListen(); }
  } else browserListen();
}
async function finishWhisper(){
  const btn = $('#talkBtn'); btn.classList.remove('listening'); btn.dataset.mode='';
  listenBusy = true; setStatus('One moment…'); setThinking(true);
  const blob = await Rec.stop();
  const text = await whisperTranscribe(blob);
  listenBusy = false; setStatus(defaultStatus());
  if(text){ addMsg('you', text); await handleCommand(text); }
  else { setThinking(false); addMsg('lifeos', "I could not hear that. Let me try another way."); browserListen(); return; }
  resumeWake();
}
function browserListen(){
  const btn = $('#talkBtn'); btn.classList.add('listening'); btn.dataset.mode='browser';
  setStatus('Listening…');
  Voice.listen(
    (best)=>{ if(best){ addMsg('you', best); handleCommand(best); } },
    ()=>{ btn.classList.remove('listening'); btn.dataset.mode=''; setStatus(defaultStatus()); resumeWake(); }
  );
}
function defaultStatus(){ return state.settings.wake ? 'Say “LifeOS”, or tap to talk' : 'Tap to talk'; }

function wire(){
  // talk button — Whisper (record→transcribe) when a key is set, else browser voice
  $('#talkBtn').onclick = () => onTalk();

  // quick-action chips
  $$('[data-act]').forEach(c=> c.onclick = ()=>{
    const a=c.dataset.act;
    if(a==='medicines'){ renderMeds(); go('medicines'); }
    else if(a==='call'){ renderContacts(); go('call'); }
    else if(a==='torch'){ Torch.toggle(); addMsg('lifeos','Here is the torch.'); }
    else if(a==='youtube'){ openYouTube(''); }
    else if(a==='help'){ speakAndShow(say('help_intro')); }
    else if(a==='repeat'){ if(lastSpoken) Voice.speak(lastSpoken); }
  });

  // back buttons
  $$('[data-back]').forEach(b=> b.onclick=()=>go('home'));
  $('#setupBtn').onclick = ()=>{ fillSetup(); go('setup'); };

  // add medicine
  $('#addMedBtn').onclick = ()=> openForm('Add a medicine',
    [ {name:'name', label:'Medicine name', placeholder:'e.g. Blood pressure pill'},
      {name:'times', label:'Times to take', type:'times'} ],
    d=>{ if(!d.name) return; state.medicines.push({id:uid(), name:d.name, times:d.times||[], taken:null}); persist(); renderMeds(); });

  // add contact
  $('#addContactBtn').onclick = ()=> openForm('Add a person',
    [ {name:'name', label:'Name (what you call them)', placeholder:'e.g. Beta, Vishal, Doctor'},
      {name:'phone', label:'Phone number (with country code)', type:'tel', placeholder:'+91...'} ],
    d=>{ if(!d.name||!d.phone) return; state.contacts.push({id:uid(), name:d.name, phone:d.phone}); persist(); renderContacts(); });

  // reminder buttons
  $('#tookBtn').onclick = verifyTaken;
  $('#snoozeBtn').onclick = snoozeReminder;

  // setup controls
  const langSel=$('#langSel');
  LANGS.forEach(([code,label])=>{ const o=document.createElement('option'); o.value=code; o.textContent=label; langSel.appendChild(o); });
  langSel.onchange = ()=>{ state.settings.lang=langSel.value; persist(); Voice.speak(say('greet')); };
  $('#rateSel').oninput = e=>{ state.settings.rate=parseFloat(e.target.value); persist(); };
  $('#testVoice').onclick = ()=> Voice.speak(say('greet'));
  $('#userName').oninput = e=>{ state.settings.name=e.target.value.trim(); persist(); };
  $('#groqKey').oninput = e=>{ state.settings.groqKey=e.target.value.trim(); persist(); };

  // hands-free wake word
  $('#wakeToggle').onchange = e=>{
    state.settings.wake = e.target.checked; persist();
    if(state.settings.wake){ Wake.start(); toast('Hands-free is on. Just say “LifeOS”.'); }
    else Wake.stop();
    setStatus(defaultStatus());
  };
  // caregiver management (moved out of the elder's home screen)
  $('#openMeds').onclick = ()=>{ renderMeds(); go('medicines'); };
  $('#openPeople').onclick = ()=>{ renderContacts(); go('call'); };
}
function fillSetup(){
  $('#langSel').value=state.settings.lang;
  $('#rateSel').value=state.settings.rate;
  $('#userName').value=state.settings.name;
  $('#groqKey').value=state.settings.groqKey||'';
  $('#wakeToggle').checked=!!state.settings.wake;
}

/* ============================================================
   START
   ============================================================ */
function start(){
  wire();
  $('#hello').textContent = state.settings.name ? `Hello, ${state.settings.name} 👋` : 'Hello 👋';
  setStatus(defaultStatus());
  tickClock(); setInterval(tickClock, 15000);
  checkReminders(); setInterval(checkReminders, 20000);

  // ask for notification permission once (best-effort)
  if('Notification' in window && Notification.permission==='default'){
    setTimeout(()=>Notification.requestPermission(), 3000);
  }
  // register service worker for offline + install
  if('serviceWorker' in navigator){ navigator.serviceWorker.register('sw.js').catch(()=>{}); }

  // greet + start hands-free on first tap (browsers need a user gesture for audio/mic)
  const greetOnce = ()=>{
    Voice.speak(say('greet'));
    if(state.settings.wake) Wake.start();
    document.removeEventListener('click', greetOnce);
  };
  document.addEventListener('click', greetOnce, {once:true});
}
document.addEventListener('DOMContentLoaded', start);
