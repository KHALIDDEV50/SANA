import React,{useEffect,useRef,useState} from 'react';
import {API_MODE,api,aiApi} from '../lib/data';
import {tr} from '../lib/i18n';
import {moodboardReferenceUrl} from '../lib/creativeTools';
const request=(method,data)=>({method,body:JSON.stringify(data)});
const tools=['BUSINESS_MODEL_BUILDER','TARGET_AUDIENCE_BUILDER','SWOT_ANALYZER','COMPETITOR_ANALYSIS','RISK_ANALYZER'];
export default function DomainPanels({project,lang,mode}){
 const t=k=>tr(lang,k),ar=lang==='ar';
 const creative=['writing','games'].includes(project.domain);
 const available=creative?[['timeline','timeline']]:project.domain==='business'?[['business','businessReports'],['indicators','saudiIndicators']]:[];
 const [tab,setTab]=useState(mode==='timeline'?'timeline':available[0]?.[0]||'');
 const [error,setError]=useState(''),[busy,setBusy]=useState(false),[query,setQuery]=useState(''),[books,setBooks]=useState([]),[book,setBook]=useState(null),[tool,setTool]=useState(tools[0]),[input,setInput]=useState(''),[report,setReport]=useState(null),[indicators,setIndicators]=useState(null),[timeline,setTimeline]=useState([]),[order,setOrder]=useState('presentation');
 const alive=useRef(true);useEffect(()=>{alive.current=true;return()=>{alive.current=false}},[]);
 async function act(fn){setBusy(true);setError('');try{await fn()}catch(e){if(alive.current)setError(e.message)}finally{if(alive.current)setBusy(false)}}
 if(!API_MODE)return <section className="spark-panel"><p>{t('noBackend')}</p></section>;
 return <section className="spark-domain-panels"><header><span className="eyebrow">SANA</span><h1>{creative?(ar?'التسلسل الزمني':'Timeline'):t('businessReports')}</h1></header>{!creative&&<nav className="spark-panel-tabs">{available.map(([id,key])=><button key={id} className={tab===id?'selected':''} onClick={()=>setTab(id)}>{t(key)}</button>)}</nav>}{error&&<p role="alert" className="ux-error">{error}</p>}
 {tab==='timeline'&&<div className="spark-panel"><div className="toolbar"><select value={order} onChange={e=>setOrder(e.target.value)}>{['presentation','chronological'].map(k=><option key={k} value={k}>{t(k)}</option>)}</select><button className="btn btn-primary" disabled={busy||!project.activeOutputId||!project.storyMap} onClick={()=>act(async()=>setTimeline(await api(`/api/v1/story-event/timeline/${order}/${project.activeOutputId}`)))}>{t('refresh')}</button></div><ol className="spark-timeline">{(Array.isArray(timeline)?timeline:[]).map((e,i)=><li key={e.id||i}><strong>{e.title}</strong><p>{e.description}</p><small>{e.inWorldTime||e[`${order}Order`]||'—'}</small></li>)}</ol></div>}
 {tab==='business'&&<form className="spark-panel" onSubmit={e=>{e.preventDefault();act(async()=>{const inputJson=JSON.stringify({description:input});let run=await api('/api/business/tools',request('POST',{projectId:project.id,outputId:project.activeOutputId,baseRevision:project.graphRevision,toolType:tool,inputJson}));setReport(null);for(let i=0;i<120&&alive.current&&['PENDING','RUNNING'].includes(run.status);i++){await new Promise(r=>setTimeout(r,2000));if(!alive.current)return;run=await aiApi.status(run.statusUrl)}if(!alive.current)return;if(run.status!=='COMPLETED')throw Error(run.error?.message||run.status);setReport(run.result)})}}><label className="form-field"><span>{t('businessReports')}</span><select value={tool} onChange={e=>setTool(e.target.value)}>{tools.map((k,i)=><option key={k} value={k}>{ar?['نموذج العمل','الجمهور المستهدف','تحليل SWOT','تحليل المنافسين','تحليل المخاطر'][i]:k.replaceAll('_',' ')}</option>)}</select></label><label className="form-field"><span>{t('reportInput')}</span><textarea required maxLength={15000} rows={5} value={input} onChange={e=>setInput(e.target.value)}/></label><button className="btn btn-primary" disabled={busy||!project.activeOutputId}>{t(busy?'awaitingResult':'runReport')}</button>{report&&<Report value={report} lang={lang}/>}</form>}
 {tab==='indicators'&&<section className="spark-panel"><p>{ar?'بيانات مرجعية من البنك الدولي؛ ليست تحليل سوق أو بيانات يستهلكها AI تلقائيًا.':'World Bank reference data, not market analysis or automatically supplied AI context.'}</p><button className="btn btn-primary" disabled={busy} onClick={()=>act(async()=>setIndicators(await api('/api/v1/external/world-bank/saudi-indicators')))}>{t('refresh')}</button>{indicators&&<Report value={indicators} lang={lang}/>}</section>}
 </section>
}
function Report({value,lang}){if(typeof value==='string'&&/^[\[{]/.test(value.trim())){try{const parsed=JSON.parse(value);return <Report value={parsed} lang={lang}/>}catch{}}if(value==null)return null;if(typeof value!=='object')return <p className="spark-report-value">{String(value)}</p>;if(Array.isArray(value))return <ul>{value.map((v,i)=><li key={i}><Report value={v} lang={lang}/></li>)}</ul>;return <dl className="spark-report">{Object.entries(value).map(([k,v])=><div key={k}><dt>{tr(lang,k).replaceAll('_',' ')}</dt><dd><Report value={v}/></dd></div>)}</dl>}


/** Separate, project-aware book inspiration tool. Open Library remains the authoritative source. */
export function BookInspiration({project,lang}){
 const ar=lang==='ar',say=(a,b)=>ar?a:b;
 const [query,setQuery]=useState(''),[focus,setFocus]=useState(''),[results,setResults]=useState([]),[selected,setSelected]=useState(null);
 const [draft,setDraft]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const mounted=useRef(true);
 useEffect(()=>{mounted.current=true;return()=>{mounted.current=false}},[]);
 const base=`/api/projects/${project.id}/creativity/assist`;
 if(!API_MODE)return <section className="spark-panel"><p>{say("هذه الأداة تحتاج اتصالًا بالخادم.","Connect the backend to use this tool.")}</p></section>;
 const pause=ms=>new Promise(done=>setTimeout(done,ms));
 async function search(e){e.preventDefault();setBusy(true);setError('');setSelected(null);
  try{const r=await api(`/api/v1/creativity/books/search?keyword=${encodeURIComponent(query.trim())}&maxResults=18&startIndex=0`);if(mounted.current)setResults(r.books||[])}
  catch(e){if(mounted.current)setError(e.message)}finally{if(mounted.current)setBusy(false)}
 }
 async function inspire(){setBusy(true);setError('');setDraft(null);
  try{
   let next=await api(`${base}/books`,{method:'POST',body:JSON.stringify({focus:focus.trim()})});
   if(mounted.current)setDraft(next);
   for(let attempts=0;attempts<55&&mounted.current&&!['PENDING','FAILED','APPLIED'].includes(next.status);attempts++){
    await pause(2000);if(!mounted.current)return;
    next=await api(`${base}/drafts/${encodeURIComponent(next.draftId)}`);
    if(mounted.current)setDraft(next);
   }
   if(mounted.current&&!['PENDING','FAILED'].includes(next.status))setError(say('انتهى الانتظار؛ أعيدي المحاولة لاحقًا.','Timed out; try again later.'));
   if(next.status==='FAILED'&&mounted.current)setError(next.error||'AI request failed');
  }catch(e){if(mounted.current)setError(e.message)}finally{if(mounted.current)setBusy(false)}
 }
 const card=(book,reason)=>{const url=moodboardReferenceUrl(book.previewLink),cover=moodboardReferenceUrl(book.thumbnail);
  return <article key={book.id} className="sana-book-card">
   <div className="sana-book-cover">{cover?<img src={cover} loading="lazy" alt="" referrerPolicy="no-referrer" onError={e=>{e.currentTarget.style.visibility='hidden'}}/>:<div className="sana-book-cover-empty">📖</div>}</div>
   <div className="sana-book-info"><h3>{book.title}</h3><p>{(book.authors||[]).join('، ')||say('مؤلف غير محدد','Unknown author')}</p><small>{book.publishedDate||'—'}</small>
    {reason&&<p className="sana-book-reason">✦ {reason}</p>}
    <div className="sana-book-links"><button type="button" onClick={async()=>{setError('');try{setSelected(await api(`/api/v1/creativity/books/get/${encodeURIComponent(book.id)}`))}catch(e){setError(e.message)}}}>{say('عن الكتاب','About the book')}</button>
     {url&&<a href={url} target="_blank" rel="noopener noreferrer">Open Library ↗</a>}</div></div>
  </article>;
 };
 return <section className="ux-tool-page sana-books-page" dir={ar?'rtl':'ltr'}>
  <header className="ux-tool-head"><div className="ux-tool-heading"><span className="ux-tool-icon">📚</span><div><small>SANA · CREATIVE LIBRARY</small><h1>{say('الكتب الملهمة','Inspiring books')}</h1><p>{say('مراجع حقيقية تساعدك تطوّرين قصتك وعالمك.','Real books to inspire your story and world.')}</p></div></div></header>
  <div className="sana-book-inspire"><h2>✦ {say('خلّي سنا تختار لمشروعك','Let SANA recommend books')}</h2><p>{say('سنا تقرأ شخصيات مشروعك وعالمه وحبكته، وتختار من نتائج Open Library الحقيقية.','SANA reads your story and selects verified Open Library books.')}</p>
   <label>{say('تبين تركز على شيء معيّن؟ (اختياري)','Optional focus')}<textarea rows={2} maxLength={350} value={focus} onChange={e=>setFocus(e.target.value)} placeholder={say('مثل: كتب تساعدني أكتب حبكة تحقيق غامضة','e.g. psychological mystery and clues')}/></label>
   <button className="btn btn-primary" type="button" disabled={busy} onClick={inspire}>{busy?say('سنا تبحث وتقرأ النتائج...','SANA is researching...'):say('اقترحي كتبًا من مشروعي ✨','Find books for my project ✨')}</button>
   {draft&&<p role="status" className="sana-quiet">{draft.status==='SEARCH_PLANNING'?say('سنا تفهم المشروع...','Reading project...'):draft.status==='SELECTING'?say('سنا تقارن الكتب المتاحة...','Evaluating verified books...'):draft.status==='PENDING'?say('هذه ترشيحات سنا.','Here are SANA’s suggestions.'):draft.status==='FAILED'?draft.error:''}</p>}
  </div>
  {error&&<p className="ux-error" role="alert">{error}</p>}
  {draft?.status==='PENDING'&&<section className="sana-book-section"><h2>{say('اختيارات سنا لمشروعك','SANA recommendations')}</h2><p>{draft.summary}</p><div className="sana-book-grid">{(draft.books||[]).map(p=>card(p.book,p.reason))}</div>{!draft.books?.length&&<p>{say('ما لقيت كتب مناسبة كفاية من النتائج. جرّبي وصفًا آخر.','No sufficiently relevant books found.')}</p>}</section>}
  <section className="sana-book-section"><h2>{say('ابحثي بنفسك','Search the library')}</h2><form className="sana-book-search" onSubmit={search}><input required maxLength={200} value={query} onChange={e=>setQuery(e.target.value)} placeholder={say('عنوان، مؤلف، أو موضوع...','Title, author, or topic...')}/><button type="submit" className="btn btn-primary" disabled={busy}>{say('بحث','Search')}</button></form>
   <div className="sana-book-grid">{results.map(b=>card(b,null))}</div>
  </section>
  {selected&&<section className="sana-book-detail"><button type="button" onClick={()=>setSelected(null)}>× {say('إغلاق','Close')}</button><h2>{selected.title}</h2><p>{selected.description||say('لا يوجد وصف متاح لهذا الكتاب.','No description available.')}</p></section>}
  <p className="sana-quiet">{say('الترشيحات للقراءة والإلهام، وليست نسخًا أو إعادة إنتاج لمحتوى الكتب.','Recommendations are for inspiration, not reproduction.')}</p>
 </section>;
}

// Project Understanding is an OPTIONAL AI context, not a required project setup step.
// UI uses ordinary lines; preserve existing complex JSON records without changing their structure.
const summaryFields=['actorsJson','componentsJson','constraintsJson'];
const emptySummary=()=>({projectGoal:'',actorsJson:'',componentsJson:'',constraintsJson:''});
function decodeSummary(raw){
 if(!raw)return {text:'',locked:false};
 try{
  const value=JSON.parse(raw);
  if(Array.isArray(value)&&value.every(x=>typeof x==='string'))return {text:value.join('\n'),locked:false};
  if(typeof value==='string')return {text:value,locked:false};
  return {text:'',locked:true};
 }catch{return {text:raw,locked:false}}
}
export function ProjectSummary({projectId,lang}){
 const ar=lang==='ar',say=(a,e)=>ar?a:e;
 const [expanded,setExpanded]=useState(false),[loaded,setLoaded]=useState(false),[saved,setSaved]=useState(null);
 const [draft,setDraft]=useState(emptySummary),[busy,setBusy]=useState(false),[error,setError]=useState(''),[message,setMessage]=useState('');
 const [locked,setLocked]=useState({});
 async function load(){
  setBusy(true);setError('');
  try{
   const result=await api(`/api/v1/project-understanding/get/project/${projectId}`);
   setSaved(result);
   setDraft({projectGoal:result.projectGoal||'',...Object.fromEntries(summaryFields.map(key=>[key,decodeSummary(result[key]).text]))});
   setLocked(Object.fromEntries(summaryFields.map(key=>[key,decodeSummary(result[key]).locked])));
  }catch(e){
   if(e.status===404||/project understanding not found/i.test(e.message)){
    setSaved(null);setDraft(emptySummary());setLocked({});
   }else setError(e.message);
  }finally{setLoaded(true);setBusy(false)}
 }
 async function save(e){
  e.preventDefault();setMessage('');setError('');
  if(!draft.projectGoal.trim()){
   setError(say('أضف هدف المشروع قبل حفظ الملخص، أو اتركه فارغًا دون حفظ.','Add a project goal to save this optional summary.'));
   return;
  }
  setBusy(true);
  try{
   const payload={projectGoal:draft.projectGoal.trim()};
   for(const key of summaryFields){
    // Legacy complex JSON is read-only and passed through unchanged.
    payload[key]=locked[key]?saved?.[key]||null:JSON.stringify(draft[key].split('\n').map(x=>x.trim()).filter(Boolean));
   }
   await api(`/api/v1/project-understanding/${saved?`update/${saved.id}`:`add/${projectId}`}`,request(saved?'PUT':'POST',payload));
   setMessage(say('تم حفظ ملخص المشروع.','Project summary saved.'));
   await load();
  }catch(e){setError(e.message)}finally{setBusy(false)}
 }
 async function remove(){
  if(!saved||!window.confirm(say('حذف ملخص المشروع المحفوظ؟ لن يتغير محتوى المشروع.','Delete the saved summary? Project content stays unchanged.')))return;
  setBusy(true);setError('');
  try{await api(`/api/v1/project-understanding/delete/${saved.id}`,{method:'DELETE'});setSaved(null);setDraft(emptySummary());setLocked({});setMessage(say('تم حذف الملخص.','Summary deleted.'))}
  catch(e){setError(e.message)}finally{setBusy(false)}
 }
 return <details className="sana-project-summary" open={expanded} onToggle={e=>{const isOpen=e.currentTarget.open;setExpanded(isOpen);if(isOpen&&!loaded&&API_MODE)load()}}>
  <summary>{say('ملخص المشروع (اختياري)','Project summary (optional)')}</summary>
  <p>{say('يساعد سنا على فهم هدف المشروع ومتطلباته. لا تحتاج إلى تعبئته لاستخدام أدوات المشروع أو الذكاء الاصطناعي.','Helps SANA understand your goals and constraints. It is not required to use the project or AI.')}</p>
  {!API_MODE?<p>{say('يتاح الحفظ عند الاتصال بالخادم.','Saving is available when connected to the backend.')}</p>:
  <form className="sana-summary-form" onSubmit={save}>
   {!loaded&&<p>{say('جارٍ تحميل الملخص...','Loading summary...')}</p>}
   {loaded&&<><label className="form-field"><span>{say('هدف المشروع','Project goal')}</span><textarea rows={3} maxLength={10000} value={draft.projectGoal} onChange={e=>setDraft({...draft,projectGoal:e.target.value})} placeholder={say('ما الذي تريد تحقيقه من هذا المشروع؟','What should this project achieve?')}/></label>
   {summaryFields.map((key,i)=><label className="form-field" key={key}><span>{(ar?['المشاركون أو الجمهور','الأجزاء الأساسية','المتطلبات والقيود']:['People / audience','Main components','Requirements & constraints'])[i]}</span>
    {locked[key]?<p className="sana-summary-legacy">{say('توجد بيانات منظمة محفوظة مسبقًا في هذا الحقل. ستُحفظ كما هي دون تعديل.','This field contains existing structured data; it will be preserved unchanged.')}</p>:<textarea rows={2} maxLength={10000} value={draft[key]} onChange={e=>setDraft({...draft,[key]:e.target.value})} placeholder={say('اكتب عنصرًا في كل سطر (اختياري)','One item per line (optional)')}/>}
   </label>)}
   <div className="modal-actions"><button type="submit" className="btn btn-primary" disabled={busy}>{say('حفظ الملخص','Save summary')}</button>{saved&&<button type="button" className="btn btn-ghost" disabled={busy} onClick={remove}>{say('حذف الملخص','Delete summary')}</button>}</div></>}
   {error&&<p className="ux-error" role="alert">{error}</p>}{message&&<p className="sana-summary-success" role="status">{message}</p>}
  </form>}
 </details>;
}
