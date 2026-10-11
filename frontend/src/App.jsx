import { useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { ArrowLeft, ArrowUpRight, BarChart3, Check, ChevronRight, CircleHelp, ClipboardList, Plus, Scale, Search, Sparkles, Trash2, Trophy, X } from 'lucide-react'

function App() {
  const [decisions, setDecisions] = useState([])
  const [activeId, setActiveId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [results, setResults] = useState([])
  const [view, setView] = useState('overview')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [criterionName, setCriterionName] = useState('')
  const [weight, setWeight] = useState(3)
  const [alternativeName, setAlternativeName] = useState('')
  const [alternativeDescription, setAlternativeDescription] = useState('')

  const refreshList = async () => setDecisions(await api.decisions())
  const refreshDetail = async (id = activeId) => {
    if (!id) return
    const [d, r] = await Promise.all([api.decision(id), api.results(id)])
    setDetail(d); setResults(r)
  }
  useEffect(() => { api.decisions().then(setDecisions).catch(e => setError(e.message)).finally(() => setLoading(false)) }, [])
  useEffect(() => {
    if (!activeId) return
    setBusy(true); setError('')
    refreshDetail(activeId).catch(e => setError(e.message)).finally(() => setBusy(false))
  }, [activeId])
  const filtered = useMemo(() => decisions.filter(d => `${d.title} ${d.description || ''}`.toLowerCase().includes(search.toLowerCase())), [decisions, search])
  const notify = (message) => { setToast(message); window.setTimeout(() => setToast(''), 2600) }
  const run = async (action, success) => {
    setError('')
    try { await action(); await refreshList(); if (activeId) await refreshDetail(activeId); notify(success) }
    catch (e) { setError(e.message) }
  }
  const create = async (e) => {
    e.preventDefault()
    if (!title.trim()) return
    await run(async () => { const d = await api.createDecision({ title: title.trim(), description }); setTitle(''); setDescription(''); setShowCreate(false); setActiveId(d.id); setView('workspace') }, 'Decision created')
  }
  const addCriterion = async (e) => {
    e.preventDefault()
    if (!criterionName.trim()) return
    await run(async () => { await api.addCriterion(activeId, { name: criterionName.trim(), weight: Number(weight) }); setCriterionName('') }, 'Criterion added')
  }
  const addAlternative = async (e) => {
    e.preventDefault()
    if (!alternativeName.trim()) return
    await run(async () => { await api.addAlternative(activeId, { name: alternativeName.trim(), description: alternativeDescription }); setAlternativeName(''); setAlternativeDescription('') }, 'Alternative added')
  }
  const saveScore = async (alternativeId, criterionId, score) => {
    await run(() => api.saveScore(activeId, { alternativeId, criterionId, score: Number(score) }), 'Score saved')
  }
  const deleteDecision = async (id) => {
    if (!window.confirm('Delete this decision and all its criteria, alternatives, and scores?')) return
    await run(async () => { await api.deleteDecision(id); if (activeId === id) { setActiveId(null); setDetail(null); setView('overview') } }, 'Decision deleted')
  }
  const openDecision = (id) => { setActiveId(id); setView('workspace') }
  const winner = results[0]
  // Scores are requested from the API for ranking; the detail endpoint is intentionally compact.
  const [scoreGrid, setScoreGrid] = useState({})
  useEffect(() => {
    if (!activeId || !detail) return
    const saved = {}
    ;(detail.scores || []).forEach(s => { saved[`${s.alternativeId}:${s.criterionId}`] = String(s.score) })
    setScoreGrid(prev => ({ ...prev, [activeId]: { ...saved, ...(prev[activeId] || {}) } }))
  }, [activeId, detail])
  const gridKey = (a, c) => `${a}:${c}`
  const currentGrid = scoreGrid[activeId] || {}
  const loadError = error && error.toLowerCase().includes('failed to fetch')

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><Scale size={21}/></div><div><strong>DecisionMate</strong><span>SMART DECISIONS</span></div></div>
      <div className="workspace-label">WORKSPACE</div>
      <button className={`nav-item ${view === 'overview' ? 'selected' : ''}`} onClick={() => { setView('overview'); setActiveId(null) }}><BarChart3 size={18}/> Overview</button>
      <button className={`nav-item ${view === 'workspace' ? 'selected' : ''}`} onClick={() => detail && setView('workspace')} disabled={!detail}><ClipboardList size={18}/> Decision builder</button>
      <div className="side-divider"/>
      <div className="side-section-head"><span>YOUR DECISIONS</span><button className="icon-btn tiny" onClick={() => setShowCreate(true)} aria-label="Add decision"><Plus size={16}/></button></div>
      <div className="side-list">
        {decisions.slice(0, 7).map(d => <button key={d.id} className={`side-decision ${activeId === d.id ? 'active' : ''}`} onClick={() => openDecision(d.id)}><span className="decision-dot"/><span className="side-decision-text">{d.title}</span><ChevronRight size={14}/></button>)}
        {!decisions.length && !loading && <p className="side-empty">Your decisions will appear here.</p>}
      </div>
      <div className="sidebar-bottom"><div className="help-icon"><CircleHelp size={18}/></div><div><strong>Make it a clear choice</strong><p>Compare what matters most to you.</p></div></div>
    </aside>

    <main className="main">
      <header className="topbar"><div className="breadcrumb"><span>Workspace</span><ChevronRight size={14}/><strong>{view === 'overview' ? 'Overview' : detail?.title || 'Decision builder'}</strong></div><div className="topbar-right"><span className="status-dot"/> All changes saved <div className="avatar">DM</div></div></header>
      <div className="page-content">
        {error && <div className="alert"><span>{loadError ? 'Cannot connect to the API. Start Spring Boot and check MySQL, then refresh.' : error}</span><button onClick={() => setError('')}><X size={16}/></button></div>}
        {loading ? <div className="loading"><div className="spinner"/>Loading your workspace…</div> : view === 'overview' || !activeId ? <>
          <div className="eyebrow"><Sparkles size={14}/> DECISION INTELLIGENCE</div>
          <div className="hero-row"><div><h1>Better decisions,<br/><span>made with clarity.</span></h1><p className="hero-copy">Bring your options together, weigh what matters, and find the best fit for you.</p></div><div className="hero-art"><div className="art-circle c1"/><div className="art-circle c2"/><div className="art-card"><div className="art-check"><Check size={18}/></div><div className="art-lines"><i/><i/><i/></div><div className="art-score">8.7 <span>/ 10</span></div></div><div className="art-spark">✦</div></div></div>
          <div className="stats-grid"><div className="stat-card"><div className="stat-icon purple"><ClipboardList size={18}/></div><span className="stat-label">Total decisions</span><strong>{decisions.length}</strong><small>Created in your workspace</small></div><div className="stat-card"><div className="stat-icon green"><Trophy size={18}/></div><span className="stat-label">Ready to compare</span><strong>{decisions.filter(d => d.criteriaCount > 0 && d.alternativesCount > 1).length}</strong><small>With criteria and options</small></div><div className="stat-card"><div className="stat-icon orange"><Scale size={18}/></div><span className="stat-label">Scoring method</span><strong>Weighted</strong><small>Importance × score</small></div></div>
          <section className="section">
            <div className="section-heading"><div><h2>Your decisions</h2><p>Pick up where you left off or start fresh.</p></div><button className="primary-btn" onClick={() => setShowCreate(true)}><Plus size={17}/> New decision</button></div>
            <div className="search-wrap"><Search size={17}/><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search decisions…" /></div>
            {filtered.length ? <div className="decision-grid">{filtered.map((d, i) => <article className="decision-card" key={d.id}><div className="decision-card-top"><div className={`decision-icon color-${i%4}`}><Scale size={20}/></div><button className="icon-btn" onClick={() => deleteDecision(d.id)} title="Delete decision"><Trash2 size={16}/></button></div><h3>{d.title}</h3><p className="card-desc">{d.description || 'No description yet. Open this decision to add criteria and alternatives.'}</p><div className="card-meta"><span><span className="mini-dot"/>{d.criteriaCount} criteria</span><span>{d.alternativesCount} alternatives</span></div><button className="card-open" onClick={() => openDecision(d.id)}>Open decision <ArrowUpRight size={16}/></button></article>)}</div> : <div className="empty-state"><div className="empty-illustration"><ClipboardList size={27}/><Plus size={16}/></div><h3>{search ? 'No matching decisions' : 'Your next great decision starts here'}</h3><p>{search ? 'Try another search term.' : 'Create a decision, add your criteria, then compare alternatives with a transparent score.'}</p>{!search && <button className="primary-btn" onClick={() => setShowCreate(true)}><Plus size={17}/> Create your first decision</button>}</div>}
          </section>
          <div className="tip-banner"><div className="tip-icon"><Sparkles size={19}/></div><div><strong>A simple way to get started</strong><p>List the things that matter most, give each one an importance weight, and score every option consistently.</p></div><span className="tip-number">01 / 03</span></div>
        </> : detail && <section className="workspace">
          <button className="back-link" onClick={() => { setView('overview'); setActiveId(null) }}><ArrowLeft size={16}/> All decisions</button>
          <div className="workspace-title-row"><div><div className="eyebrow">DECISION WORKSPACE</div><h1>{detail.title}</h1><p>{detail.description || 'Add criteria and alternatives to start comparing your options.'}</p></div><button className="outline-btn" onClick={() => setView(view === 'results' ? 'workspace' : 'results')}><BarChart3 size={16}/>{view === 'results' ? 'Edit decision' : 'View results'}</button></div>
          <div className="stepper"><div className="step active"><span>1</span><div><strong>Criteria</strong><small>What matters?</small></div></div><div className="step-line"/><div className="step active"><span>2</span><div><strong>Alternatives</strong><small>What are the options?</small></div></div><div className="step-line"/><div className={`step ${view === 'results' ? 'active' : ''}`}><span>3</span><div><strong>Results</strong><small>Compare scores</small></div></div></div>
          {view === 'results' ? <div className="panel results-panel"><div className="panel-heading"><div><h2><Trophy size={19}/> Your results</h2><p>Weighted scores out of 10. Higher is better.</p></div></div>{(!detail.criteria.length || !detail.alternatives.length) ? <div className="inline-empty">Add at least one criterion and one alternative to see results.</div> : <><div className="winner-card"><div className="winner-medal"><Trophy size={23}/></div><div><span className="winner-label">TOP-RANKED ALTERNATIVE</span><h3>{winner?.alternativeName || 'Scores not available'}</h3><p>{winner ? 'Based on the importance weights and scores entered.' : 'Add scores to calculate a ranking.'}</p></div><div className="winner-score">{winner?.weightedScore.toFixed(2) ?? '—'}<small>/ 10</small></div></div><div className="result-list">{results.map((r, i) => <div className="result-row" key={r.alternativeId}><div className={`rank-badge ${i===0?'first':''}`}>{r.rank}</div><div className="result-name"><strong>{r.alternativeName}</strong><div className="score-track"><div style={{width:`${Math.min(100, r.weightedScore*10)}%`}}/></div></div><strong className="result-score">{r.weightedScore.toFixed(2)}<small> / 10</small></strong></div>)}</div><p className="result-note">Scores are weighted averages. Missing scores contribute zero to the numerator, so complete the score matrix for a fair comparison.</p></>}</div> : <>
            <div className="panel"><div className="panel-heading"><div><h2><span className="heading-step">1</span> Define your criteria</h2><p>Assign each criterion an importance from 1 (low) to 10 (high).</p></div><span className="count-pill">{detail.criteria.length} added</span></div>
              <form className="inline-form" onSubmit={addCriterion}><input required value={criterionName} onChange={e=>setCriterionName(e.target.value)} placeholder="e.g. Price, Quality, Battery life"/><label className="weight-select">Weight <select value={weight} onChange={e=>setWeight(e.target.value)}>{Array.from({length:10},(_,i)=><option key={i+1} value={i+1}>{i+1}</option>)}</select></label><button className="primary-btn compact" type="submit"><Plus size={16}/> Add</button></form>
              {detail.criteria.length > 0 && <div className="criteria-list">{detail.criteria.map(c=><div className="criteria-row" key={c.id}><div className="criteria-symbol"><Scale size={16}/></div><div className="criteria-info"><strong>{c.name}</strong><small>Importance weight</small></div><div className="weight-meter">{Array.from({length:10},(_,i)=><i key={i} className={i<c.weight?'filled':''}/>)}</div><span className="weight-value">{c.weight}</span><button className="icon-btn" onClick={()=>run(()=>api.deleteCriterion(c.id),'Criterion removed')} title="Remove criterion"><Trash2 size={15}/></button></div>)}</div>}
            </div>
            <div className="panel"><div className="panel-heading"><div><h2><span className="heading-step">2</span> Add alternatives</h2><p>These are the options you want to compare.</p></div><span className="count-pill">{detail.alternatives.length} added</span></div>
              <form className="inline-form" onSubmit={addAlternative}><input required value={alternativeName} onChange={e=>setAlternativeName(e.target.value)} placeholder="e.g. Laptop A, Laptop B"/><input value={alternativeDescription} onChange={e=>setAlternativeDescription(e.target.value)} placeholder="Short note (optional)"/><button className="primary-btn compact" type="submit"><Plus size={16}/> Add</button></form>
              {detail.alternatives.length > 0 && <div className="alternatives-list">{detail.alternatives.map((a,i)=><div className="alternative-row" key={a.id}><div className={`alt-avatar color-${i%4}`}>{a.name.slice(0,1).toUpperCase()}</div><div className="criteria-info"><strong>{a.name}</strong><small>{a.description || 'Alternative option'}</small></div><button className="icon-btn" onClick={()=>run(()=>api.deleteAlternative(a.id),'Alternative removed')} title="Remove alternative"><Trash2 size={15}/></button></div>)}</div>}
            </div>
            <div className="panel"><div className="panel-heading"><div><h2><span className="heading-step">3</span> Score each alternative</h2><p>Give each option a score from 1 (poor) to 10 (excellent) for every criterion.</p></div><span className="scale-key">1 <span/> 10</span></div>
              {!detail.criteria.length || !detail.alternatives.length ? <div className="inline-empty">Add at least one criterion and one alternative to build the score matrix.</div> : <div className="table-scroll"><table className="score-table"><thead><tr><th>Alternative</th>{detail.criteria.map(c=><th key={c.id}>{c.name}<small>Weight: {c.weight}</small></th>)}</tr></thead><tbody>{detail.alternatives.map(a=><tr key={a.id}><td><strong>{a.name}</strong></td>{detail.criteria.map(c=><td key={c.id}><select aria-label={`${a.name} score for ${c.name}`} value={currentGrid[gridKey(a.id,c.id)] ?? ''} onChange={async e=>{const val=e.target.value;setScoreGrid(prev=>({...prev,[activeId]:{...(prev[activeId]||{}),[gridKey(a.id,c.id)]:val}}));if(val) await saveScore(a.id,c.id,Number(val))}}><option value="">—</option>{Array.from({length:10},(_,i)=><option key={i+1} value={i+1}>{i+1}</option>)}</select></td>)}</tr>)}</tbody></table><p className="matrix-help">Tip: score consistently. A 10 should mean excellent performance on that criterion.</p></div>}
            </div>
            <div className="workspace-footer"><div><Check size={16}/> Weighted scoring is calculated automatically</div><button className="primary-btn" onClick={()=>setView('results')}>See ranking <ArrowUpRight size={16}/></button></div>
          </>}
        </section>}
      </div>
    </main>

    {showCreate && <div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)setShowCreate(false)}}><form className="modal" onSubmit={create}><div className="modal-top"><div className="modal-icon"><Plus size={19}/></div><button type="button" className="icon-btn" onClick={()=>setShowCreate(false)}><X size={18}/></button></div><h2>Create a decision</h2><p>Give your decision a clear name and add a little context.</p><label>Decision name<input autoFocus required maxLength="160" value={title} onChange={e=>setTitle(e.target.value)} placeholder="e.g. Choose a laptop"/></label><label>Description <span className="optional">(optional)</span><textarea maxLength="1200" rows="3" value={description} onChange={e=>setDescription(e.target.value)} placeholder="What are you trying to decide?"/></label><div className="modal-actions"><button type="button" className="outline-btn" onClick={()=>setShowCreate(false)}>Cancel</button><button type="submit" className="primary-btn"><Plus size={16}/> Create decision</button></div></form></div>}
    {toast && <div className="toast"><Check size={16}/>{toast}</div>}
  </div>
}
export default App
