'use strict';

const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const byId = id => document.getElementById(id);

const storage = {
  get(key, fallback) {
    try { const v = localStorage.getItem(`novahire.v4.${key}`); return v ? JSON.parse(v) : fallback; }
    catch { return fallback; }
  },
  set(key, value) {
    try { localStorage.setItem(`novahire.v4.${key}`, JSON.stringify(value)); } catch {}
  }
};

const state = {
  jobs: [],
  jobMap: new Map(),
  matchMap: new Map(),
  role: storage.get('role', 'candidate'),
  profile: storage.get('profile', { name: 'Pranav Kaveri', skills: 'Java, Spring, SQL, AWS', location: 'Hyderabad', title: 'Backend Developer' }),
  saved: new Set(storage.get('saved', [1, 5])),
  compare: new Set(),
  applications: storage.get('applications', [
    { jobId: 2, status: 'Under Review', date: 'Aug 19, 2026' },
    { jobId: 4, status: 'Shortlisted', date: 'Aug 16, 2026' },
    { jobId: 7, status: 'Applied', date: 'Aug 14, 2026' }
  ]),
  lens: 'fit',
  currentJobs: []
};

const companyInfo = {
  Amazon: ['Technology', '100K+ employees', 'Build systems used at massive scale.'],
  Microsoft: ['Technology', '100K+ employees', 'Cloud, AI and developer platforms.'],
  Google: ['Technology', '100K+ employees', 'Products, research and global infrastructure.'],
  Deloitte: ['Consulting', '100K+ employees', 'Technology and business transformation.'],
  Oracle: ['Enterprise Software', '100K+ employees', 'Cloud and database technology.'],
  TCS: ['IT Services', '100K+ employees', 'Digital engineering and enterprise services.'],
  Accenture: ['Consulting', '100K+ employees', 'Technology, operations and transformation.'],
  Infosys: ['IT Services', '100K+ employees', 'Digital services and consulting.'],
  NVIDIA: ['AI & Semiconductors', '25K+ employees', 'Accelerated computing and AI systems.'],
  Salesforce: ['Enterprise Software', '50K+ employees', 'CRM and cloud business platforms.'],
  Adobe: ['Software', '25K+ employees', 'Creative, document and digital experience tools.'],
  IBM: ['Technology', '100K+ employees', 'Hybrid cloud, AI and enterprise systems.'],
  Intel: ['Semiconductors', '100K+ employees', 'Compute platforms and hardware innovation.'],
  Qualcomm: ['Semiconductors', '50K+ employees', 'Wireless, mobile and edge computing.'],
  Flipkart: ['E-commerce', '10K+ employees', 'Commerce, logistics and consumer technology.'],
  Swiggy: ['Consumer Tech', '5K+ employees', 'On-demand delivery and logistics technology.'],
  PhonePe: ['Fintech', '5K+ employees', 'Payments and financial technology.'],
  Zoho: ['SaaS', '10K+ employees', 'Business software built for global customers.']
};

const applicants = [
  { name:'Aarav Sharma', code:'AS', skills:['Java','Spring','SQL','AWS'], experience:2.1, education:'B.Tech CSE', projects:5, match:96, recency:9 },
  { name:'Meera Rao', code:'MR', skills:['Java','Spring','Docker','SQL'], experience:1.6, education:'B.Tech IT', projects:4, match:92, recency:7 },
  { name:'Ishaan Verma', code:'IV', skills:['Java','REST API','Azure','SQL'], experience:2.4, education:'B.Tech CSE', projects:6, match:89, recency:6 },
  { name:'Saanvi Reddy', code:'SR', skills:['Java','Spring','REST API','Git'], experience:1.2, education:'B.Tech CSE', projects:5, match:86, recency:5 },
  { name:'Rohan Kapoor', code:'RK', skills:['Python','Java','SQL','AWS'], experience:2.8, education:'B.E. CSE', projects:3, match:81, recency:3 },
  { name:'Ananya Nair', code:'AN', skills:['Java','SQL','Docker','Kubernetes'], experience:1.9, education:'B.Tech IT', projects:4, match:84, recency:4 }
];

function qs(obj) {
  return Object.entries(obj).filter(([,v]) => v !== undefined && v !== null && v !== '').map(([k,v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
}

async function api(path, params = {}, options = {}) {
  const url = `${path}${Object.keys(params).length ? `?${qs(params)}` : ''}`;
  const res = await fetch(url, options);
  const json = await res.json().catch(() => ({}));
  if (!res.ok || json.ok === false) throw new Error(json.error || 'Request failed');
  return json;
}

function escapeHtml(value = '') {
  return String(value).replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#039;','"':'&quot;'}[c]));
}

function initials(company) {
  return company.split(/\s+/).map(x => x[0]).join('').slice(0,2).toUpperCase();
}

function derive(job) {
  const modes = ['Hybrid','On-site','Remote'];
  const types = job.experienceYears === 0 && job.id % 7 === 0 ? 'Internship' : (job.id % 11 === 0 ? 'Contract' : 'Full-time');
  const workMode = job.workMode || modes[job.id % modes.length];
  const applicants = 18 + ((job.id * 37 + job.salaryLpa * 3) % 188);
  const postedDays = 1 + ((job.id * 3) % 14);
  const competition = applicants < 65 ? 'Low' : applicants < 130 ? 'Medium' : 'High';
  const category = /machine|ai|deep learning|data scientist/i.test(job.title) ? 'AI & Data' :
    /data analyst|business analyst|etl/i.test(job.title) ? 'Data Analytics' :
    /cloud|devops|site reliability/i.test(job.title) ? 'Cloud & DevOps' :
    /frontend|full stack|javascript|ui/i.test(job.title) ? 'Web Engineering' :
    /security|cyber/i.test(job.title) ? 'Cybersecurity' : 'Software Engineering';
  return { ...job, workMode, jobType: types, applicants, postedDays, competition, category, verified: true, deadlineDays: 7 + ((job.id * 2) % 24) };
}

function matchFor(jobId) {
  return state.matchMap.get(Number(jobId)) || { score: 64 + ((Number(jobId) * 7) % 24), skillScore: 70, locationScore: 70, titleScore: 65, matchedSkills: [], missingSkills: [] };
}

function competitionClass(level) { return level.toLowerCase(); }
function roleViewDefault(role) { return role === 'recruiter' ? 'recruiter' : role === 'admin' ? 'admin' : 'home'; }
function showToast(message) {
  const el = byId('toast'); el.textContent = message; el.classList.add('show'); clearTimeout(showToast.t); showToast.t = setTimeout(() => el.classList.remove('show'), 2300);
}

function syncBadges() {
  byId('savedBadge').textContent = state.saved.size;
  byId('appBadge').textContent = state.applications.length;
  byId('metricSaved').textContent = state.saved.size;
  byId('metricApplications').textContent = state.applications.length;
  byId('compareCount').textContent = state.compare.size;
}

function saveState() {
  storage.set('saved', [...state.saved]); storage.set('applications', state.applications); storage.set('profile', state.profile); storage.set('role', state.role); syncBadges();
}

function setView(name) {
  $$('.view').forEach(v => v.classList.toggle('active', v.dataset.view === name));
  $$('.side-nav button[data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === name));
  window.scrollTo({top:0, behavior:'smooth'});
  if (innerWidth < 821) byId('sidebar').classList.remove('open');
  if (name === 'saved') renderSaved();
  if (name === 'applications') renderApplications();
  if (name === 'companies') renderCompanies();
  if (name === 'insights') renderInsights();
  if (name === 'applicants') renderApplicants();
}

function switchRole(role) {
  state.role = role; saveState();
  byId('candidateNav').hidden = role !== 'candidate'; byId('recruiterNav').hidden = role !== 'recruiter'; byId('adminNav').hidden = role !== 'admin';
  const cfg = role === 'candidate' ? ['PK','Pranav K.','Job Seeker'] : role === 'recruiter' ? ['AT','Abhay T.','Recruiter'] : ['AD','NovaHire Admin','Administrator'];
  byId('roleAvatar').textContent = cfg[0]; byId('roleName').textContent = cfg[1]; byId('roleLabel').textContent = cfg[2];
  byId('profileShortcut').querySelector('span').textContent = cfg[0];
  byId('profileShortcut').querySelector('b').textContent = role === 'admin' ? 'Admin' : cfg[1].split(' ')[0];
  byId('globalSearch').placeholder = role === 'candidate' ? 'Search jobs, skills or companies' : role === 'recruiter' ? 'Search jobs or candidates' : 'Search companies, reports or users';
  byId('smartAlertsBtn').style.display = role === 'candidate' ? '' : 'none';
  byId('roleMenu').hidden = true; setView(roleViewDefault(role));
}

function renderJobCard(job, compact = false) {
  const m = matchFor(job.id), d = derive(job), saved = state.saved.has(job.id), comparing = state.compare.has(job.id);
  if (compact) return `<article class="job-card compact" data-job-id="${job.id}">
    <div class="company-logo">${escapeHtml(initials(job.company))}</div>
    <div class="job-main"><div class="job-title-line"><h4>${escapeHtml(job.title)}</h4><span class="verified" title="Verified employer">✓</span></div><div class="job-company">${escapeHtml(job.company)} · ${escapeHtml(job.location)}</div><div class="job-meta"><span>₹${job.salaryLpa} LPA</span><span>${d.workMode}</span><span>${job.experienceYears ? `${job.experienceYears}+ yrs` : 'Fresher'}</span></div></div>
    <div class="match-badge"><b>${Math.round(m.score)}%</b><small>match</small></div>
  </article>`;
  return `<article class="job-card full" data-job-id="${job.id}">
    <div class="job-card-top"><div class="job-card-left"><div class="company-logo">${escapeHtml(initials(job.company))}</div><div><div class="job-title-line"><h3>${escapeHtml(job.title)}</h3><span class="verified" title="Verified employer">✓</span></div><div class="job-company">${escapeHtml(job.company)} · ${escapeHtml(job.location)}</div></div></div>
      <div class="job-card-actions"><button class="save-btn ${saved?'active':''}" data-save="${job.id}" title="Save job">${saved?'♥':'♡'}</button><button class="compare-btn ${comparing?'active':''}" data-compare="${job.id}" title="Compare job">⇄</button></div></div>
    <div class="fit-row"><div class="fit-score"><b>${Math.round(m.score)}%</b><span>match</span></div><div class="fit-bar"><i style="--fit:${Math.round(m.score)}%"></i></div></div>
    <div class="job-meta"><span>₹${job.salaryLpa} LPA</span><span>${d.workMode}</span><span>${d.jobType}</span><span>${job.experienceYears ? `${job.experienceYears}+ yrs` : 'Fresher friendly'}</span></div>
    <div class="job-skill-row">${job.skills.slice(0,4).map(s=>`<span>${escapeHtml(s)}</span>`).join('')}</div>
    <div class="job-card-footer"><div><span class="competition ${competitionClass(d.competition)}">● ${d.competition} competition</span><small> · ${d.applicants} applicants</small></div><button class="open-job" data-open-job="${job.id}">View details →</button></div>
  </article>`;
}

async function loadCore() {
  try {
    const health = await api('/api/health'); byId('engineText').textContent = `${health.jobs} live opportunities`; byId('engineText').parentElement.querySelector('i').style.background = '#4ade80';
    const data = await api('/api/jobs', {limit: 100}); state.jobs = data.jobs; state.currentJobs = data.jobs; state.jobMap = new Map(data.jobs.map(j => [j.id, j]));
    await refreshMatches();
    renderHome(); renderJobs(state.jobs); renderCompanies(); renderRecruiter(); renderAdmin(); renderApplications(); renderInsights(); syncBadges();
  } catch (e) {
    byId('engineText').textContent = 'Connection unavailable';
    showToast(`Could not load portal data: ${e.message}`);
  }
}

async function refreshMatches() {
  try {
    const r = await api('/api/recommend', { name: state.profile.name, skills: state.profile.skills, location: state.profile.location, title: state.profile.title, limit: 30 });
    state.matchMap.clear(); r.results.forEach(x => state.matchMap.set(x.job.id, x));
    const strong = r.results.filter(x => x.score >= 80).length; byId('metricStrong').textContent = strong;
    if (r.results[0]) { byId('heroMatchValue').textContent = `${Math.round(r.results[0].score)}%`; byId('heroSignal').textContent = `${r.results[0].job.title} is your strongest match today`; }
  } catch {}
}

function renderHome() {
  const ranked = [...state.jobs].sort((a,b) => matchFor(b.id).score - matchFor(a.id).score).slice(0,4);
  byId('recommendedHome').classList.remove('loading-block'); byId('recommendedHome').innerHTML = ranked.map(j => renderJobCard(j,true)).join('');
  const companies = companyStats().slice(0,5); byId('homeCompanies').innerHTML = companies.map(c => `<button class="company-mini" data-company="${escapeHtml(c.name)}"><div class="company-logo">${escapeHtml(initials(c.name))}</div><div><b>${escapeHtml(c.name)}</b><span>${c.jobs} open role${c.jobs>1?'s':''}</span></div></button>`).join('');
  const missing = matchFor(ranked[0]?.id).missingSkills || []; byId('skillUnlockChips').innerHTML = (missing.length ? missing.slice(0,3) : ['Docker','Kubernetes']).map(s=>`<span>${escapeHtml(s)}</span>`).join('');
}

function applyFilters(baseJobs = state.jobs) {
  const mode = byId('filterMode').value, exp = byId('filterExperience').value, salary = Number(byId('filterSalary').value||0), type = byId('filterType').value, sort = byId('sortJobs').value;
  const location = byId('jobsLocation').value.trim().toLowerCase();
  let out = baseJobs.filter(j => {
    const d=derive(j); return (!mode || d.workMode===mode) && (exp==='' || j.experienceYears>=Number(exp)) && j.salaryLpa>=salary && (!type || d.jobType===type) && (!location || j.location.toLowerCase().includes(location));
  });
  out.sort((a,b)=> sort==='salary' ? b.salaryLpa-a.salaryLpa : sort==='newest' ? derive(a).postedDays-derive(b).postedDays : sort==='competition' ? derive(a).applicants-derive(b).applicants : matchFor(b.id).score-matchFor(a.id).score);
  return out;
}

function renderJobs(jobs = state.jobs, assist = '') {
  state.currentJobs = applyFilters(jobs); byId('jobsCount').textContent = `${state.currentJobs.length} opportunit${state.currentJobs.length===1?'y':'ies'}`; byId('searchAssist').textContent = assist;
  byId('jobsList').innerHTML = state.currentJobs.length ? state.currentJobs.map(j=>renderJobCard(j)).join('') : '<div class="empty-card"><b>No jobs found</b>Try removing a filter or searching a broader skill.</div>';
}

async function runJobSearch(e) {
  e?.preventDefault(); const q = byId('jobsQuery').value.trim();
  if (!q) { renderJobs(state.jobs); return; }
  try {
    const exact = await api('/api/search', {q, algo:'kmp'});
    if (exact.jobs.length) { renderJobs(exact.jobs, `Smart search matched “${q}”`); return; }
    const fuzzy = await api('/api/fuzzy', {q, distance:3});
    if (fuzzy.jobs.length) renderJobs(fuzzy.jobs, `Showing close matches for “${q}”`); else renderJobs([], `No close matches for “${q}”`);
  } catch(e2){ showToast(e2.message); }
}

function companyStats() {
  const map = new Map(); state.jobs.forEach(j => { const c=map.get(j.company)||{name:j.company,jobs:0,locations:new Set(),maxSalary:0}; c.jobs++; c.locations.add(j.location); c.maxSalary=Math.max(c.maxSalary,j.salaryLpa); map.set(j.company,c); });
  return [...map.values()].sort((a,b)=>b.jobs-a.jobs || b.maxSalary-a.maxSalary);
}

function renderCompanies() {
  const html = companyStats().map(c => { const info=companyInfo[c.name]||['Technology','1K+ employees','Growing teams and new opportunities.']; return `<article class="company-card"><div class="company-card-top"><div class="company-logo">${escapeHtml(initials(c.name))}</div><span class="verified-employer">✓ Verified Employer</span></div><h3>${escapeHtml(c.name)}</h3><p>${escapeHtml(info[2])}</p><div class="company-card-stats"><div><small>Open roles</small><b>${c.jobs}</b></div><div><small>Top salary</small><b>₹${c.maxSalary} LPA</b></div></div><button class="btn secondary full" data-company="${escapeHtml(c.name)}">View opportunities</button></article>`; }).join('');
  byId('companyGrid').innerHTML = html;
}

function renderSaved() {
  const jobs = [...state.saved].map(id=>state.jobMap.get(id)).filter(Boolean); byId('savedList').innerHTML = jobs.length ? jobs.map(j=>renderJobCard(j)).join('') : '<div class="empty-card"><b>Your saved list is empty.</b>Save jobs while browsing to create a focused shortlist.</div>';
}

function renderApplications() {
  const stages = ['Applied','Under Review','Shortlisted','Interview','Selected'];
  const counts = Object.fromEntries(stages.map(x=>[x,0])); state.applications.forEach(a=> { if(counts[a.status]!==undefined) counts[a.status]++; });
  const colors=['#5b5df0','#3b82f6','#7c5cff','#f59e0b','#19a974']; byId('applicationPipeline').innerHTML = stages.map((s,i)=>`<article><small>${s}</small><b>${counts[s]}</b><i style="--c:${colors[i]}"></i></article>`).join('');
  const rows=[...state.applications].reverse().map(a=>{const j=state.jobMap.get(a.jobId); if(!j)return''; const cls=a.status.toLowerCase().replace(/\s+/g,'-'); return `<div class="application-row"><div><b>${escapeHtml(j.title)}</b><span>${escapeHtml(j.company)}</span></div><span>${escapeHtml(j.location)}</span><span>${a.date}</span><span class="status-pill ${cls}">${a.status}</span><button class="text-btn" data-open-job="${j.id}">Details →</button></div>`}).join('');
  byId('applicationsTable').innerHTML = rows || '<div class="empty-card"><b>No applications yet.</b>Find a strong match and apply when you are ready.</div>';
  const successful=state.applications.filter(a=>['Shortlisted','Interview','Selected'].includes(a.status)).length; byId('successRate').textContent = state.applications.length ? `${Math.round(successful/state.applications.length*100)}%` : '—';
}

async function buildApplicationStrategy() {
  const button = byId('buildStrategyBtn');
  const result = byId('strategyResult');
  const hours = Number(byId('strategyHours').value || 8);
  button.disabled = true;
  button.textContent = 'Building plan…';
  result.innerHTML = '<div class="strategy-loading">Finding the strongest set of opportunities for your available time…</div>';
  try {
    const data = await api('/api/optimize', {
      name: state.profile.name,
      skills: state.profile.skills,
      location: state.profile.location,
      title: state.profile.title,
      hours
    });
    const jobs = data.exact?.jobs || [];
    const used = data.exact?.totalHours ?? 0;
    if (!jobs.length) {
      result.innerHTML = '<div class="empty-strategy"><span>◌</span><div><b>No focused plan yet.</b><small>Complete your skills and preferences, then try again.</small></div></div>';
      return;
    }
    result.innerHTML = `<div class="strategy-summary"><div><small>FOCUSED PLAN</small><b>${jobs.length} opportunities</b><span>${used} of ${hours} hours planned</span></div><div class="strategy-fit"><b>${Math.round(jobs.reduce((s,x)=>s+(x.matchScore||0),0)/jobs.length)}%</b><small>average fit</small></div></div><div class="strategy-jobs">${jobs.map((x,i)=>`<article data-open-job="${x.job.id}"><span>${String(i+1).padStart(2,'0')}</span><div><b>${escapeHtml(x.job.title)}</b><small>${escapeHtml(x.job.company)} · ${escapeHtml(x.job.location)}</small></div><em>${Math.round(x.matchScore)}%</em></article>`).join('')}</div>`;
  } catch (err) {
    result.innerHTML = `<div class="empty-strategy"><span>!</span><div><b>Could not build the plan.</b><small>${escapeHtml(err.message || 'Try again in a moment.')}</small></div></div>`;
  } finally {
    button.disabled = false;
    button.textContent = 'Build my plan';
  }
}

function renderInsights() {
  const cats = new Map(), skills = new Map(), locs = new Map(); state.jobs.forEach(j=>{const d=derive(j),m=matchFor(j.id).score; cats.set(d.category,(cats.get(d.category)||0)+m); j.skills.forEach(s=>skills.set(s,(skills.get(s)||0)+(m>75?1:0))); locs.set(j.location,(locs.get(j.location)||0)+m);});
  const topCats=[...cats.entries()].sort((a,b)=>b[1]-a[1]).slice(0,5); const max=Math.max(...topCats.map(x=>x[1]),1); byId('careerBars').innerHTML=topCats.map(([k,v])=>`<div><span>${escapeHtml(k)}</span><i style="--w:${Math.round(v/max*100)}%"></i><b>${Math.round(v/max*100)}%</b></div>`).join('');
  if(topCats[0]) byId('bestCategory').textContent=topCats[0][0]; const topSkill=[...skills.entries()].sort((a,b)=>b[1]-a[1])[0]; if(topSkill) byId('topSkill').textContent=topSkill[0]; const topLoc=[...locs.entries()].sort((a,b)=>b[1]-a[1])[0]; if(topLoc) byId('bestLocation').textContent=topLoc[0]; renderLens();
}

function renderLens() {
  let jobs=[...state.jobs];
  jobs.sort((a,b)=> state.lens==='salary' ? b.salaryLpa-a.salaryLpa : state.lens==='competition' ? derive(a).applicants-derive(b).applicants : state.lens==='growth' ? (matchFor(b.id).score+b.salaryLpa*1.2)-(matchFor(a.id).score+a.salaryLpa*1.2) : matchFor(b.id).score-matchFor(a.id).score);
  byId('lensJobs').innerHTML=jobs.slice(0,3).map(j=>`<button class="mini-job" data-open-job="${j.id}"><div class="company-logo">${initials(j.company)}</div><div><b>${escapeHtml(j.title)}</b><span>${escapeHtml(j.company)} · ${state.lens==='salary'?`₹${j.salaryLpa} LPA`:state.lens==='competition'?`${derive(j).competition} competition`:`${Math.round(matchFor(j.id).score)}% match`}</span></div></button>`).join('');
}

async function runSkillUnlock() {
  const btn=byId('skillUnlockBtn'); const old=btn.textContent; btn.textContent='Finding best skill…'; btn.disabled=true;
  try { const r=await api('/api/skill-plan',{name:state.profile.name,skills:state.profile.skills,location:state.profile.location,title:state.profile.title,budget:2}); const skills=r.selectedSkills||[]; byId('skillUnlockChips').innerHTML=skills.map(s=>`<span>${escapeHtml(s)}</span>`).join('')||'<span>Profile already broad</span>'; byId('skillUnlockCopy').textContent=skills.length?`Learning ${skills.join(' + ')} could widen access to your strongest current opportunities.`:'Your current skill set already covers many of your strongest matches.'; showToast('Career skill plan updated'); }
  catch(e){showToast(e.message)} finally{btn.textContent=old;btn.disabled=false}
}

function openJob(id) {
  const job=state.jobMap.get(Number(id)); if(!job)return; const d=derive(job),m=matchFor(job.id); const matched=m.matchedSkills?.length?m.matchedSkills:job.skills.filter((s,i)=>i<Math.ceil(job.skills.length/2)); const missing=m.missingSkills||job.skills.filter(s=>!matched.includes(s));
  byId('jobDrawerContent').innerHTML=`<div class="job-detail-head"><div class="company-logo">${initials(job.company)}</div><div><div class="job-title-line"><h2>${escapeHtml(job.title)}</h2><span class="verified">✓</span></div><p>${escapeHtml(job.company)} · ${escapeHtml(job.location)} · ${d.workMode}</p></div></div>
  <div class="drawer-match"><div class="match-ring" style="--match:${Math.round(m.score)}%"><b>${Math.round(m.score)}%</b><small>match</small></div><div><span class="eyebrow">SMART MATCH</span><h3 style="margin:5px 0 4px">${m.score>=85?'Excellent fit for your profile':m.score>=70?'Strong potential match':'Worth exploring'}</h3><p style="font-size:9px;color:#7d8898;margin:0">Fit considers your skills, preferred location and target role.</p></div></div>
  <div class="why-apply"><h3>Why should you apply?</h3><div class="why-grid"><div class="why-item good"><b>${matched.length}/${job.skills.length} required skills match</b><span>${matched.slice(0,3).join(', ')||'Relevant core skills'}</span></div><div class="why-item good"><b>${m.locationScore>=90?'Location matches':'Location is flexible'}</b><span>${escapeHtml(job.location)} · ${d.workMode}</span></div><div class="why-item good"><b>Salary visibility</b><span>₹${job.salaryLpa} LPA listed</span></div><div class="why-item ${missing.length?'gap':'good'}"><b>${missing.length?'Skill gap detected':'No major skill gap'}</b><span>${missing.slice(0,2).join(', ')||'Strong coverage'}</span></div></div></div>
  <div class="detail-section"><h3>Required skills</h3><div class="detail-skill-grid">${job.skills.map(s=>`<span class="${matched.includes(s)?'matched':'missing'}">${matched.includes(s)?'✓':'+'} ${escapeHtml(s)}</span>`).join('')}</div></div>
  <div class="detail-section"><h3>About the role</h3><p>${escapeHtml(job.description)}. You will collaborate with engineering and product teams, own meaningful work and contribute to reliable production systems.</p></div>
  <div class="detail-section"><h3>Role details</h3><div class="why-grid"><div class="why-item"><b>Experience</b><span>${job.experienceYears?`${job.experienceYears}+ years`:'Fresher friendly'}</span></div><div class="why-item"><b>Job type</b><span>${d.jobType}</span></div><div class="why-item"><b>Competition</b><span>${d.competition} · ${d.applicants} applicants</span></div><div class="why-item"><b>Apply by</b><span>Within ${d.deadlineDays} days</span></div></div></div>
  <div class="drawer-actions"><button class="btn primary" data-apply="${job.id}">${state.applications.some(a=>a.jobId===job.id)?'Application submitted':'Apply now'}</button><button class="btn secondary" data-save="${job.id}">${state.saved.has(job.id)?'♥ Saved':'♡ Save'}</button></div>`;
  byId('drawerBackdrop').hidden=false; byId('jobDrawer').classList.add('open'); byId('jobDrawer').setAttribute('aria-hidden','false');
}

function closeDrawers() { byId('drawerBackdrop').hidden=true; byId('jobDrawer').classList.remove('open'); byId('compareDrawer').classList.remove('open'); byId('jobDrawer').setAttribute('aria-hidden','true'); byId('compareDrawer').setAttribute('aria-hidden','true'); }

function toggleSave(id) { id=Number(id); state.saved.has(id)?state.saved.delete(id):state.saved.add(id); saveState(); renderJobs(state.currentJobs.length?state.currentJobs:state.jobs); renderSaved(); renderHome(); showToast(state.saved.has(id)?'Job saved to your shortlist':'Removed from saved jobs'); if(byId('jobDrawer').classList.contains('open')) openJob(id); }
function toggleCompare(id) { id=Number(id); if(state.compare.has(id)) state.compare.delete(id); else if(state.compare.size<3) state.compare.add(id); else return showToast('Compare up to 3 jobs at a time'); syncBadges(); renderJobs(state.currentJobs.length?state.currentJobs:state.jobs); showToast(state.compare.has(id)?'Added to comparison':'Removed from comparison'); }
function applyToJob(id) { id=Number(id); if(state.applications.some(a=>a.jobId===id)) return showToast('You already applied to this role'); state.applications.push({jobId:id,status:'Applied',date:new Date().toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})}); saveState(); renderApplications(); showToast('Application submitted successfully'); openJob(id); }

function openCompare() {
  const jobs=[...state.compare].map(id=>state.jobMap.get(id)).filter(Boolean); if(jobs.length<2)return showToast('Select at least 2 jobs to compare');
  const rows=[
    ['Opportunity', ...jobs.map(j=>`<b>${escapeHtml(j.title)}</b><span>${escapeHtml(j.company)}</span>`)],
    ['Match', ...jobs.map(j=>`${Math.round(matchFor(j.id).score)}%`)],
    ['Salary', ...jobs.map(j=>`₹${j.salaryLpa} LPA`)],
    ['Location', ...jobs.map(j=>escapeHtml(j.location))],
    ['Work mode', ...jobs.map(j=>derive(j).workMode)],
    ['Experience', ...jobs.map(j=>j.experienceYears?`${j.experienceYears}+ years`:'Fresher friendly')],
    ['Competition', ...jobs.map(j=>`${derive(j).competition} (${derive(j).applicants})`)],
    ['Skill gap', ...jobs.map(j=>(matchFor(j.id).missingSkills||[]).slice(0,2).join(', ')||'No major gap')]
  ];
  byId('compareContent').innerHTML=`<div class="compare-table" style="--cols:${jobs.length}">${rows.map((r,i)=>`<div class="compare-row ${i===0?'head':''}">${r.map(x=>`<div>${x}</div>`).join('')}</div>`).join('')}</div>`; byId('compareDrawer').classList.add('open'); byId('drawerBackdrop').hidden=false; byId('compareDrawer').setAttribute('aria-hidden','false');
}

function renderRecruiter() {
  const jobs=state.jobs.slice(0,5); byId('recruiterOpenings').innerHTML=jobs.map((j,i)=>`<div class="opening-row"><div><b>${escapeHtml(j.title)}</b><span>${derive(j).applicants} applicants · ${j.vacancies} vacancies</span></div><div class="opening-progress"><div><i style="--p:${Math.min(100,35+i*12)}%"></i></div><small>${8+i*3} shortlisted</small></div></div>`).join('');
  byId('topCandidates').innerHTML=applicants.slice(0,4).map(c=>`<div class="candidate-mini"><div class="candidate-avatar">${c.code}</div><div><b>${c.name}</b><span>${c.skills.slice(0,3).join(' · ')}</span></div><strong>${c.match}%</strong></div>`).join('');
  byId('recruiterJobsTable').innerHTML=jobs.map(j=>`<div class="recruiter-job-row"><div><b>${escapeHtml(j.title)}</b><span>${escapeHtml(j.location)} · ${derive(j).workMode}</span></div><span>${derive(j).applicants} applicants</span><span>${j.vacancies} vacancies</span><span>Active</span><div class="row-actions"><button>View</button><button>Edit</button><button>Close</button></div></div>`).join(''); renderApplicants(); renderInterviews();
}

function renderApplicants() {
  const anon=byId('anonymousToggle')?.checked, sort=byId('applicantSort')?.value||'match'; let list=[...applicants]; list.sort((a,b)=>sort==='experience'?b.experience-a.experience:sort==='recent'?b.recency-a.recency:b.match-a.match);
  byId('applicantGrid').innerHTML=list.map((c,i)=>`<article class="applicant-card"><div class="applicant-head"><div class="applicant-id"><div class="candidate-avatar">${anon?`#${1048+i}`:c.code}</div><div><b>${anon?`Candidate #${1048+i}`:escapeHtml(c.name)}</b><span>${anon?'Identity hidden until shortlist':escapeHtml(c.education)}</span></div></div><div class="applicant-score"><b>${c.match}%</b><small>match</small></div></div><div class="applicant-skills">${c.skills.map(s=>`<span>${escapeHtml(s)}</span>`).join('')}</div><div class="applicant-stats"><div><small>Experience</small><b>${c.experience} yrs</b></div><div><small>Projects</small><b>${c.projects}</b></div><div><small>Education</small><b>${anon?'Verified':escapeHtml(c.education.replace('B.Tech ','').replace('B.E. ',''))}</b></div></div><div class="applicant-actions"><button class="shortlist-btn" data-shortlist="${i}">Shortlist</button><button class="reject-btn">Not now</button></div></article>`).join('');
}

function renderInterviews() {
  const items=[['10:30','Today','Meera Rao','Java Backend Developer','Technical Round'],['14:00','Today','Aarav Sharma','Software Engineer','Hiring Manager'],['11:00','Tomorrow','Saanvi Reddy','Java Backend Developer','Technical Round'],['16:30','Aug 24','Ishaan Verma','Cloud Engineer','Final Discussion']]; byId('interviewList').innerHTML=items.map(x=>`<article class="interview-card"><div class="interview-time"><b>${x[0]}</b><span>${x[1]}</span></div><div><h3>${x[2]} · ${x[3]}</h3><p>${x[4]} · 45 min · Video interview</p></div><button class="btn secondary">View details</button></article>`).join('');
}

const adminCompanies=[['Vertex Labs','Technology','Official domain pending'],['BluePeak Systems','SaaS','Identity documents submitted'],['Northstar Analytics','Analytics','Recruiter email review']];
const adminReports=[['Suspicious salary claim','Job #2841','High'],['Duplicate posting','Job #1973','Medium'],['Inappropriate recruiter message','User #8032','High']];
function renderAdmin() {
  byId('adminQueueMini').innerHTML=adminCompanies.map(x=>`<article><div><b>${x[0]}</b><span>${x[1]} · ${x[2]}</span></div><em>Pending</em></article>`).join(''); byId('adminReportsMini').innerHTML=adminReports.map(x=>`<article><div><b>${x[0]}</b><span>${x[1]}</span></div><em>${x[2]}</em></article>`).join('');
  byId('verificationList').innerHTML=adminCompanies.map(x=>`<div class="admin-row"><div><b>${x[0]}</b><span>${x[1]}</span></div><span>${x[2]}</span><span>2 documents</span><span>Pending</span><div><button data-admin-action>Review</button></div></div>`).join(''); byId('reportsList').innerHTML=adminReports.map(x=>`<div class="admin-row"><div><b>${x[0]}</b><span>${x[1]}</span></div><span>User report</span><span>${x[2]}</span><span>Open</span><div><button data-admin-action>Investigate</button></div></div>`).join('');
}

function bindEvents() {
  document.addEventListener('click', e=>{
    const nav=e.target.closest('[data-view]'); if(nav && (nav.closest('.side-nav'))) setView(nav.dataset.view);
    const jump=e.target.closest('[data-view-jump]'); if(jump)setView(jump.dataset.viewJump);
    const open=e.target.closest('[data-open-job]'); if(open){e.stopPropagation();openJob(open.dataset.openJob)}
    const jobCard=e.target.closest('.job-card[data-job-id]'); if(jobCard && !e.target.closest('button'))openJob(jobCard.dataset.jobId);
    const save=e.target.closest('[data-save]'); if(save){e.stopPropagation();toggleSave(save.dataset.save)}
    const comp=e.target.closest('[data-compare]'); if(comp){e.stopPropagation();toggleCompare(comp.dataset.compare)}
    const apply=e.target.closest('[data-apply]'); if(apply){e.stopPropagation();applyToJob(apply.dataset.apply)}
    const company=e.target.closest('[data-company]'); if(company){byId('jobsQuery').value=company.dataset.company;setView('jobs');runJobSearch();}
    const quick=e.target.closest('[data-quick]'); if(quick){setView('jobs'); if(quick.dataset.quick==='Remote'){byId('jobsQuery').value='';byId('filterMode').value='Remote';renderJobs(state.jobs,'Showing remote opportunities');}else{byId('jobsQuery').value=quick.dataset.quick;runJobSearch();}}
    const role=e.target.closest('[data-role]'); if(role)switchRole(role.dataset.role);
    const shortlist=e.target.closest('[data-shortlist]'); if(shortlist)showToast('Candidate shortlisted — identity can now be revealed');
    if(e.target.closest('[data-admin-action]'))showToast('Review opened');
  });
  byId('roleMenuBtn').addEventListener('click',()=>byId('roleMenu').hidden=!byId('roleMenu').hidden);
  byId('mobileMenu').addEventListener('click',()=>byId('sidebar').classList.toggle('open'));
  byId('heroSearchForm').addEventListener('submit',e=>{e.preventDefault();byId('jobsQuery').value=byId('heroQuery').value;byId('jobsLocation').value=byId('heroLocation').value;setView('jobs');runJobSearch();});
  byId('jobSearchForm').addEventListener('submit',runJobSearch); ['filterMode','filterExperience','filterSalary','filterType','sortJobs'].forEach(id=>byId(id).addEventListener('change',()=>renderJobs(state.currentJobs.length?state.currentJobs:state.jobs)));
  byId('jobsLocation').addEventListener('change',()=>renderJobs(state.currentJobs.length?state.currentJobs:state.jobs));
  byId('clearFilters').addEventListener('click',()=>{byId('jobSearchForm').reset();['filterMode','filterExperience','filterSalary','filterType','sortJobs'].forEach(id=>{const el=byId(id);el.selectedIndex=0});renderJobs(state.jobs);});
  byId('skillUnlockBtn').addEventListener('click',runSkillUnlock); byId('buildStrategyBtn').addEventListener('click',buildApplicationStrategy); byId('openCompareBtn').addEventListener('click',openCompare); byId('closeCompare').addEventListener('click',closeDrawers); byId('closeJobDrawer').addEventListener('click',closeDrawers); byId('drawerBackdrop').addEventListener('click',closeDrawers);
  byId('anonymousToggle').addEventListener('change',renderApplicants); byId('applicantSort').addEventListener('change',renderApplicants);
  $$('#lensOptions button').forEach(b=>b.addEventListener('click',()=>{$$('#lensOptions button').forEach(x=>x.classList.remove('active'));b.classList.add('active');state.lens=b.dataset.lens;renderLens()}));
  const notif=()=>byId('notificationPanel').hidden=!byId('notificationPanel').hidden; byId('notificationBtn').addEventListener('click',notif); byId('smartAlertsBtn').addEventListener('click',notif); byId('closeNotifications').addEventListener('click',()=>byId('notificationPanel').hidden=true);
  byId('profileShortcut').addEventListener('click',()=>state.role==='candidate'&&setView('profile'));
  const saveProfile=async e=>{e?.preventDefault();state.profile={...state.profile,title:byId('profileTitle').value.trim(),location:byId('profileLocation').value.trim(),skills:byId('profileSkills').value.trim()};saveState();await refreshMatches();renderHome();renderJobs(state.jobs);renderInsights();showToast('Profile saved — recommendations refreshed')}; byId('profileForm').addEventListener('submit',saveProfile); byId('saveProfileTop').addEventListener('click',saveProfile);
  const modal=byId('postJobModal');
  const closeModal=()=>{modal.hidden=true;modal.setAttribute('aria-hidden','true')};
  const openModal=()=>{modal.hidden=false;modal.setAttribute('aria-hidden','false');setTimeout(()=>byId('postJobTitleInput')?.focus(),0)};
  byId('postJobBtn').addEventListener('click',openModal); byId('postJobBtn2').addEventListener('click',openModal);
  $$('[data-close-modal]').forEach(b=>b.addEventListener('click',closeModal));
  modal.addEventListener('click',e=>{if(e.target===modal)closeModal()});
  byId('postJobForm').addEventListener('submit',async e=>{
    e.preventDefault();
    const form=e.currentTarget, submit=form.querySelector('button[type="submit"]');
    const payload=Object.fromEntries(new FormData(form).entries());
    submit.disabled=true; submit.textContent='Publishing…';
    try {
      const result=await api('/api/jobs',{}, {method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:qs(payload)});
      const created=result.job;
      if(created){state.jobs.unshift(created);state.jobMap.set(Number(created.id),created);state.currentJobs=[];await refreshMatches();renderRecruiter();renderJobs(state.jobs,'Latest openings');renderHome();}
      closeModal(); showToast('Job published and added to the marketplace');
    } catch(err) { showToast(err.message || 'Unable to publish job'); }
    finally { submit.disabled=false; submit.textContent='Publish job'; }
  });
  byId('globalSearch').addEventListener('keydown',e=>{if(e.key==='Enter'){byId('jobsQuery').value=e.target.value;setView('jobs');runJobSearch();}});
  document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();byId('globalSearch').focus()}if(e.key==='Escape'){closeDrawers();byId('notificationPanel').hidden=true;closeModal()}});
}

async function init(){const now=new Date();const hour=now.getHours();byId('careerDate').textContent=`${now.toLocaleDateString('en-US',{weekday:'long'}).toUpperCase()} • CAREER PULSE`;byId('welcomeGreeting').textContent=`Good ${hour<12?'morning':hour<17?'afternoon':'evening'}, Pranav.`;bindEvents();switchRole(state.role);byId('profileTitle').value=state.profile.title;byId('profileLocation').value=state.profile.location;byId('profileSkills').value=state.profile.skills;await loadCore();}
init();
