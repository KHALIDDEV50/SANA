import {TYPES, CREATIVE_TYPES, typesForDomain, findTemplate, starterNodes} from './templates';
export {TYPES};
export const API_MODE = import.meta.env.VITE_SANA_MODE === 'api';
export const USER_ID = Number(import.meta.env.VITE_SANA_USER_ID || 0);
const BASE = (import.meta.env.VITE_SANA_API_URL || '').replace(/\/$/,'');
const STORE = 'sana-demo-v1';
const now = () => new Date().toISOString();
const clone = value => JSON.parse(JSON.stringify(value));
const makeId = () => Date.now() * 100 + Math.floor(Math.random()*100);
const examples = [
 {id:101,name:'مدينة أركان',description:'في مدينة تختفي منها الذكريات، تبحث ليان عن الحقيقة وراء مكتبة لا يتذكر أحد وجودها.',domain:'writing',status:'ACTIVE',createdAt:'2026-10-01',updatedAt:'2026-10-22',elements:[
  {id:1001,type:'characters',name:'ليان',description:'بطلة تبحث عن ذكريات عائلتها.',role:'بطلة',age:24,x:110,y:115},
  {id:1002,type:'characters',name:'راشد',description:'أمين المكتبة الغامض.',role:'مساعد',age:45,x:445,y:105},
  {id:1003,type:'worlds',name:'عالم أركان',description:'عالم تتحكم فيه الذاكرة.',x:770,y:140},
  {id:1004,type:'locations',name:'مكتبة الذكريات',description:'مكتبة سرية.',x:410,y:335},
  {id:1005,type:'rules',name:'قانون الذاكرة',description:'كل سحر يفقد صاحبه ذكرى.',x:770,y:425},
  {id:1006,type:'events',name:'اكتشاف المكتبة',description:'أول دليل للغز.',presentationOrder:1,x:90,y:425}],
  relations:[{id:1,source:1001,target:1004,type:'VISITS',label:'تبحث عن'},{id:2,source:1002,target:1004,type:'LIVES_IN',label:'يحرس'},{id:3,source:1004,target:1006,type:'CONNECTED_TO',label:'مكان الحدث'}]},
 {id:102,name:'بوابة الرمال',description:'لعبة مغامرات عربية مستوحاة من الصحراء.',domain:'games',status:'ACTIVE',createdAt:'2026-10-05',updatedAt:'2026-10-20',elements:[{id:2001,type:'characters',name:'سهيل',description:'رحالة شجاع',role:'بطل',age:23,x:180,y:160},{id:2002,type:'worlds',name:'الصحراء',description:'مدن منسية',x:510,y:200}],relations:[]},
 {id:103,name:'معمارية سنا',description:'تصميم مكونات النظام وروابطها.',domain:'technology',status:'DRAFT',createdAt:'2026-10-10',updatedAt:'2026-10-20',elements:[{id:3001,type:'apis',name:'REST API',description:'Spring Boot',x:135,y:180},{id:3002,type:'services',name:'خدمة المشروع',description:'Project Service',x:430,y:180},{id:3003,type:'databases',name:'قاعدة البيانات',description:'MySQL',x:750,y:180}],relations:[{id:3011,source:3001,target:3002,type:'CONNECTED_TO',label:'يتواصل مع'},{id:3012,source:3002,target:3003,type:'CONNECTED_TO',label:'يخزن في'}]},
 {id:104,name:'فكرة متجر سعودي',description:'نموذج أولي لفكرة تجارية محلية.',domain:'business',status:'DRAFT',createdAt:'2026-10-12',updatedAt:'2026-10-20',elements:[{id:4001,type:'ideas',name:'المتجر',description:'منتجات محلية',x:150,y:160},{id:4002,type:'customers',name:'العملاء',description:'الجمهور المستهدف',x:480,y:160},{id:4003,type:'value',name:'قيمة المنتج',description:'تجربة مميزة',x:320,y:370}],relations:[]}
];
function load(){try{const parsed=JSON.parse(localStorage.getItem(STORE));if(Array.isArray(parsed))return parsed;}catch{} return clone(examples)}
function persist(projects){localStorage.setItem(STORE,JSON.stringify(projects));return projects}
function projectWithCounts(p){return {...p,elementCount:(p.elements?.length ?? p.outputs?.reduce((n,o)=>n+(o.nodes?.length||0),0) ?? 0),relationCount:p.relations?.length ?? 0}}
export async function api(path,opts={}){
 const response=await fetch(`${BASE}${path}`,{...opts,credentials:'include',headers:{'Content-Type':'application/json',...(opts.headers||{})}});
 if(!response.ok){let detail='';try{const j=await response.json();detail=j.message||j.error||j.detail||'';}catch{}throw new Error(detail||`HTTP ${response.status} — ${path}`)}
 if(response.status===204)return null;const text=await response.text();if(!text)return null;try{return JSON.parse(text)}catch{return text}
}
const json=(method,payload)=>({method,body:JSON.stringify(payload)});
const entityPaths={characters:'character-profile',worlds:'world',locations:'location',rules:'world-rule',events:'story-event'};
function normalizeGenericNode(node,outputId){let data={};try{data=JSON.parse(node.dataJson||'{}')||{}}catch{}return {id:node.id,nodeId:node.id,outputId,type:node.type?.replace(/^SANA_/,'').toLowerCase(),name:data.name||node.type||'Node',description:data.description||'',notes:data.notes||'',x:node.positionX??100,y:node.positionY??100,raw:node,generic:true};}
function normalizeCreative(e,type,outputId,node){return {id:node.id,entityId:e.id,nodeId:node.id,outputId,type,name:type==='events'?e.title:e.name,description:e.shortDescription||e.description||e.narrativeSignificance||'',role:e.role||'',age:e.age||'',locationType:e.locationType||'',eventType:e.eventType||'',presentationOrder:e.presentationOrder,chronologicalOrder:e.chronologicalOrder,notes:e.writerNotes||e.notes||'',x:node.positionX??100,y:node.positionY??100,raw:e};}
async function getApiProjects(){if(!USER_ID)throw new Error('API mode requires VITE_SANA_USER_ID (existing backend test user).');const ps=await api(`/api/v1/project/get/user/${USER_ID}`);return Array.isArray(ps)?ps:[];}
async function getApiProject(id){
 const p=await api(`/api/v1/project/get/${id}`);
 const outputs=await api(`/api/v1/project-output/get/project/${id}`);
 const realOutputs=Array.isArray(outputs)?outputs:[];
 const nodeRecords=realOutputs.filter(out=>out.representationType==='GRAPH').flatMap(out=>(out.nodes||[]).map(node=>({node,outputId:out.id})));
 const nodeMap=new Map(nodeRecords.map(x=>[x.node.id,x]));
 const creativeRecords=[];
 if(nodeRecords.some(x=>CREATIVE_TYPES.some(type=>type.toUpperCase().replace('CHARACTERS','CHARACTER').replace('WORLDS','WORLD').replace('LOCATIONS','LOCATION').replace('RULES','WORLD_RULE').replace('EVENTS','STORY_EVENT')===x.node.type))){
   // These are the existing domain CRUD endpoints; each entity carries its ProjectNode ID.
   for(const type of CREATIVE_TYPES){
     const list=await api(`/api/v1/${entityPaths[type]}/get`);
     for(const item of (Array.isArray(list)?list:[])){
       const record=nodeMap.get(item.projectNode?.id);
       if(record)creativeRecords.push(normalizeCreative(item,type,record.outputId,record.node));
     }
   }
 }
 const creativeIds=new Set(creativeRecords.map(e=>e.nodeId));
 // Generic node types are supported by ProjectNodeController for technical/business canvases.
 const generics=nodeRecords.filter(x=>!creativeIds.has(x.node.id)&&x.node.type!=='BRANCHING').map(x=>normalizeGenericNode(x.node,x.outputId));
 const all=[...creativeRecords,...generics];
 const idByNode=new Map(all.map(e=>[e.nodeId,e.id]));
 const relations=realOutputs.filter(out=>out.representationType==='GRAPH').flatMap(out=>(out.edges||[]).filter(r=>r.type!=='BRANCH_CHOICE').map(r=>({id:r.id,source:idByNode.get(r.sourceNode?.id),target:idByNode.get(r.targetNode?.id),type:r.type,label:r.label,outputId:out.id}))).filter(r=>r.source!=null&&r.target!=null);
 return {...p,outputs:realOutputs,elements:all,relations};
}
async function ensureGraphOutput(projectId,domain,existing){
 const list=existing||await api(`/api/v1/project-output/get/project/${projectId}`);
 const matching=list.find(o=>o.representationType==='GRAPH');if(matching)return matching;
 const before=new Set(list.map(x=>x.id));
 await api(`/api/v1/project-output/add/${projectId}`,json('POST',{title:domain==='technology'?'System Architecture':domain==='business'?'Business Canvas':'Creative Studio',outputType:domain==='technology'?'ARCHITECTURE':domain==='business'?'BUSINESS':'CREATIVITY',representationType:'GRAPH'}));
 const next=await api(`/api/v1/project-output/get/project/${projectId}`);
 const created=next.find(o=>!before.has(o.id));if(!created)throw new Error('Output creation did not return a graph');return created;
}
async function writeApiNode(projectId,type,values,oldId){
 const p=await getApiProject(projectId),existing=p.elements.find(e=>String(e.id)===String(oldId));
 const output=existing?.outputId?p.outputs.find(o=>o.id===existing.outputId):await ensureGraphOutput(projectId,p.domain,p.outputs);
 const x=Number(values.x??existing?.x??140),y=Number(values.y??existing?.y??135);
 if(CREATIVE_TYPES.includes(type)){
   const payload={positionX:x,positionY:y,...(type==='events'?{title:values.name}:{name:values.name}),description:values.description||'—',notes:values.notes||''};
   // Keep advanced text fields already stored by the backend when editing.
   for(const field of ['aliases','appearance','traits','strengths','weaknesses','skills','history','goals','fears','innerConflict','secrets','characterValues','characterArc','lore','atmosphere','scope','conditions','exceptions','consequences','inWorldTime'])if(existing?.raw?.[field]!=null)payload[field]=existing.raw[field];
   if(type==='characters'){payload.shortDescription=values.description||'—';payload.role=values.role||existing?.role||'OTHER';payload.age=Number(values.age)||Number(existing?.age)||18;payload.writerNotes=values.notes||'';}
   if(type==='locations'){payload.locationType=values.locationType||'';payload.narrativeSignificance=values.description||'';}
   if(type==='events'){payload.eventType=values.eventType||'';payload.presentationOrder=Number(values.presentationOrder)||1;payload.chronologicalOrder=values.chronologicalOrder?Number(values.chronologicalOrder):null;}
   await api(`/api/v1/${entityPaths[type]}/${oldId?`update/${existing.entityId}`:`add/${output.id}`}`,json(oldId?'PUT':'POST',payload));
 }else{
   const payload={type:`SANA_${type.toUpperCase()}`,positionX:x,positionY:y,dataJson:JSON.stringify({name:values.name,description:values.description||'',notes:values.notes||''})};
   await api(`/api/v1/node/${oldId?`update/${existing.nodeId}`:`add/${output.id}`}`,json(oldId?'PUT':'POST',payload));
 }
 return getApiProject(projectId);
}
export const repository={
 async list(){return API_MODE?(await getApiProjects()).map(projectWithCounts):load().map(projectWithCounts)},
 async get(id){return API_MODE?getApiProject(id):load().find(p=>String(p.id)===String(id))||null},
 async createProject(payload){
  if(API_MODE){const before=await getApiProjects();await api(`/api/v1/project/add/${USER_ID}`,json('POST',{name:payload.name,description:payload.description||'',domain:payload.domain||'writing',status:'DRAFT'}));const after=await getApiProjects();const ids=new Set(before.map(x=>x.id));const p=after.find(x=>!ids.has(x.id));if(!p)throw new Error('Project created but could not resolve its ID; refresh projects before retrying');await ensureGraphOutput(p.id,p.domain);for(const node of starterNodes(payload.templateId,payload.lang))await writeApiNode(p.id,node.type,node);return p;}
  const projects=load(),p={id:makeId(),name:payload.name,description:payload.description||'',domain:payload.domain||'writing',templateId:payload.templateId||null,status:'DRAFT',createdAt:now(),updatedAt:now(),elements:starterNodes(payload.templateId,payload.lang).map(n=>({...n,id:makeId()})),relations:[]};persist([p,...projects]);return p;
 },
 async updateProject(id,patch){
  if(API_MODE){const old=await api(`/api/v1/project/get/${id}`);await api(`/api/v1/project/update/${id}`,json('PUT',{name:patch.name??old.name,description:patch.description??old.description,domain:patch.domain??old.domain,status:patch.status??old.status,version:old.version}));return {...old,...patch};}
  const all=load().map(p=>String(p.id)===String(id)?{...p,...patch,updatedAt:now()}:p);persist(all);return all.find(p=>String(p.id)===String(id));
 },
 async removeProject(id){if(API_MODE)return api(`/api/v1/project/delete/${id}`,{method:'DELETE'});persist(load().filter(p=>String(p.id)!==String(id)));},
 async saveElement(projectId,type,values,oldId){
  if(API_MODE)return writeApiNode(projectId,type,values,oldId);
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));if(!p)throw new Error('Project not found');
  if(oldId){p.elements=p.elements.map(e=>String(e.id)===String(oldId)?{...e,...values,type}:e)}
  else p.elements.push({id:makeId(),type,...values,x:values.x??120+(p.elements.length%3)*260,y:values.y??130+Math.floor(p.elements.length/3)*165});
  p.updatedAt=now();persist(all);return p;
 },
 async removeElement(projectId,element){
  if(API_MODE){await api(`/api/v1/${CREATIVE_TYPES.includes(element.type)?entityPaths[element.type]:'node'}/delete/${CREATIVE_TYPES.includes(element.type)?element.entityId:element.nodeId}`,{method:'DELETE'});return getApiProject(projectId);}
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.elements=p.elements.filter(e=>String(e.id)!==String(element.id));p.relations=p.relations.filter(r=>r.source!==element.id&&r.target!==element.id);p.updatedAt=now();persist(all);return p;
 },
 async moveNode(projectId,id,x,y){
  if(API_MODE){const p=await getApiProject(projectId),e=p.elements.find(e=>String(e.id)===String(id));if(!e)throw new Error('Node not found');const node=p.outputs.flatMap(out=>out.nodes||[]).find(n=>n.id===e.nodeId);if(!node)throw new Error('Node coordinates unavailable');await api(`/api/v1/node/update/${e.nodeId}`,json('PUT',{type:node.type,dataJson:node.dataJson||null,positionX:x,positionY:y}));return;}
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));const e=p.elements.find(e=>String(e.id)===String(id));if(e){e.x=x;e.y=y;persist(all)}
 },
 async allowedRelationTypes(projectId,sourceId,targetId){
  if(!API_MODE)return ['CUSTOM','FAMILY','FRIENDSHIP','RIVALRY','WORK','ROMANTIC','PARTICIPATES_IN','CAUSES','AFFECTED_BY','LIVES_IN','BELONGS_TO','VISITS','LEADS_TO','PRECEDES','SIMULTANEOUS_WITH','NARRATIVE_LINK','INSIDE','CONNECTED_TO','NEAR','APPLIES_TO','RESTRICTS','AFFECTS'];
  const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(sourceId)),b=p.elements.find(e=>String(e.id)===String(targetId));
  if(!a||!b||a.outputId!==b.outputId)return ['CUSTOM'];
  if(!CREATIVE_TYPES.includes(a.type)||!CREATIVE_TYPES.includes(b.type))return ['CUSTOM','CONNECTED_TO','DEPENDS_ON','RELATES_TO'];
  const types=await api(`/api/v1/creativity/relations/get/types/${a.outputId}/${a.nodeId}/${b.nodeId}`);
  return Array.isArray(types)?types.map(t=>typeof t==='string'?t:t.name||String(t)):['CUSTOM'];
 },
 async createRelation(projectId,data){
  if(API_MODE){const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(data.source)),b=p.elements.find(e=>String(e.id)===String(data.target));if(!a||!b||a.outputId!==b.outputId)throw new Error('Both nodes must belong to the same graph output');const creativity=CREATIVE_TYPES.includes(a.type)&&CREATIVE_TYPES.includes(b.type);const path=creativity?`/api/v1/creativity/relations/add/${a.outputId}/${a.nodeId}/${b.nodeId}`:`/api/v1/edge/add/${a.outputId}/${a.nodeId}/${b.nodeId}`;const type=data.type||'CUSTOM';const label=data.label|| (type==='CUSTOM'?'Related':type.replaceAll('_',' '));await api(path,json('POST',{type,label}));return getApiProject(projectId);}
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));if(!p)throw new Error('Project not found');p.relations.push({id:makeId(),source:Number(data.source),target:Number(data.target),type:data.type||'CUSTOM',label:data.label||'Related'});p.updatedAt=now();persist(all);return p;
 },
 async removeRelation(projectId,r){if(API_MODE){await api(`/api/v1/edge/delete/${r.id}`,{method:'DELETE'});return getApiProject(projectId)}const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.relations=p.relations.filter(x=>String(x.id)!==String(r.id));p.updatedAt=now();persist(all);return p},
 async importProject(data){if(API_MODE)throw new Error('Import available in demo only');if(!data||typeof data.name!=='string'||!Array.isArray(data.elements)||!Array.isArray(data.relations))throw new Error('Invalid JSON');const p=clone(data),ids=new Map();p.id=makeId();p.elements=p.elements.map(e=>{const id=makeId();ids.set(e.id,id);return {...e,id}});p.relations=p.relations.filter(r=>ids.has(r.source)&&ids.has(r.target)).map(r=>({...r,id:makeId(),source:ids.get(r.source),target:ids.get(r.target)}));p.updatedAt=now();persist([p,...load()]);return p}
};
