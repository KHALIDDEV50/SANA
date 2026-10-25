import {TYPES, CREATIVE_TYPES, typesForDomain, starterNodes} from './templates';
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
 {id:103,name:'معمارية سنا',description:'تصميم مكونات النظام وروابطها.',domain:'technology',status:'DRAFT',createdAt:'2026-10-10',updatedAt:'2026-10-20',elements:[{id:3001,type:'application',name:'REST API',description:'Spring Boot',x:135,y:180},{id:3002,type:'service',name:'خدمة المشروع',description:'Project Service',x:430,y:180},{id:3003,type:'database',name:'قاعدة البيانات',description:'MySQL',x:750,y:180}],relations:[{id:3011,source:3001,target:3002,type:'CONNECTS_TO',label:'يتواصل مع'},{id:3012,source:3002,target:3003,type:'CONNECTS_TO',label:'يخزن في'}]},
 {id:104,name:'فكرة متجر سعودي',description:'نموذج أولي لفكرة تجارية محلية.',domain:'business',status:'DRAFT',createdAt:'2026-10-12',updatedAt:'2026-10-20',elements:[{id:4001,type:'value_proposition',name:'المتجر',description:'منتجات محلية',x:150,y:160},{id:4002,type:'customer_segment',name:'العملاء',description:'الجمهور المستهدف',x:480,y:160},{id:4003,type:'channels',name:'المتجر الإلكتروني',description:'البيع عبر الإنترنت',x:320,y:370}],relations:[]}
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

// ---------- UI <-> backend mapping ----------
// The UI keeps its own domain names; the backend has TECH, BUSINESS, EDUCATION and CREATIVITY.
const BACKEND_DOMAIN={writing:'CREATIVITY',games:'CREATIVITY',technology:'TECH',business:'BUSINESS',education:'EDUCATION'};
// The diagram a new project starts with
const PRIMARY_DIAGRAM={writing:'STORY_MAP',games:'STORY_MAP',technology:'ARCHITECTURE',business:'BUSINESS_MODEL_CANVAS',education:'MIND_MAP'};
// Creativity node types use plural UI names; every other node type is the backend name in lower case
const STORY_TYPES={characters:'CHARACTER',worlds:'WORLD',locations:'LOCATION',rules:'WORLD_RULE',events:'STORY_EVENT'};
const toBackendType=type=>STORY_TYPES[type]||String(type).toUpperCase();
const toUiType=type=>Object.keys(STORY_TYPES).find(k=>STORY_TYPES[k]===type)||String(type).toLowerCase();
export const backendDomain=domain=>BACKEND_DOMAIN[domain]||'CREATIVITY';
const uiDomainKey=id=>`sana-ui-domain-${id}`;
const activeDiagramKey=id=>`sana-active-diagram-${id}`;
const storage={get:k=>{try{return localStorage.getItem(k)}catch{return null}},set:(k,v)=>{try{localStorage.setItem(k,String(v))}catch{}}};
// writing and games are both CREATIVITY; remember which one the user picked, else guess from a branching diagram
function toUiDomain(p){
 if(p.domain==='TECH')return 'technology';if(p.domain==='BUSINESS')return 'business';if(p.domain==='EDUCATION')return 'education';
 const hint=storage.get(uiDomainKey(p.id));if(hint)return hint;
 return (p.outputs||[]).some(o=>o.outputType==='BRANCHING_STORY')?'games':'writing';
}
// Node types the add-element palette offers: the active diagram's types in API mode
export const elementTypes=project=>project?.nodeTypes?.length?project.nodeTypes:typesForDomain(project?.domain);
let diagramTypesCache=null;
export async function diagramTypes(){if(!diagramTypesCache)diagramTypesCache=await api('/api/v1/diagram/types');return diagramTypesCache||[]}

// ---------- API reads ----------
async function getApiProjects(){if(!USER_ID)throw new Error('API mode requires VITE_SANA_USER_ID (existing backend test user).');const ps=await api(`/api/v1/project/get/user/${USER_ID}`);return (Array.isArray(ps)?ps:[]).map(p=>({...p,backendDomain:p.domain,domain:toUiDomain(p)}));}
async function listDiagrams(projectId){const list=await api(`/api/v1/diagram/get/project/${projectId}`);return (Array.isArray(list)?list:[]).map(d=>({id:d.id,title:d.title,outputType:d.outputType}))}
async function createDiagram(projectId,title,diagramType){
 await api(`/api/v1/diagram/add/${projectId}`,json('POST',{title,diagramType}));
 const created=(await listDiagrams(projectId)).filter(d=>d.outputType===diagramType).sort((a,b)=>b.id-a.id)[0];
 if(!created)throw new Error('Diagram created but could not be found; reload the project');
 return created;
}
function toElement(node,outputId,storyMap,entityId,rawNode){
 const pr=node.properties||{};
 const base={id:node.id,nodeId:node.id,outputId,type:toUiType(node.type),name:pr.label||node.type,description:pr.description||pr.shortDescription||pr.narrativeSignificance||'',notes:pr.notes||pr.writerNotes||'',x:node.positionX??100,y:node.positionY??100,properties:pr,raw:pr,rawNode};
 if(!storyMap)return base;
 return {...base,entityId,role:pr.role||'',age:pr.age||'',locationType:pr.locationType||'',eventType:pr.eventType||'',presentationOrder:pr.presentationOrder,chronologicalOrder:pr.chronologicalOrder};
}
// A project shows one diagram at a time on its map: the one picked last, else the domain's main diagram
async function getApiProject(id){
 const p=await api(`/api/v1/project/get/${id}`);
 const domain=toUiDomain(p);
 const types=await diagramTypes();
 let diagrams=await listDiagrams(id);
 const canvas=diagrams.filter(d=>d.outputType!=='BRANCHING_STORY');
 let active=canvas.find(d=>String(d.id)===storage.get(activeDiagramKey(id)))||canvas.find(d=>d.outputType===PRIMARY_DIAGRAM[domain])||canvas[0];
 if(!active){active=await createDiagram(id,p.name,PRIMARY_DIAGRAM[domain]);diagrams=[...diagrams,active]}
 const rules=types.find(t=>t.diagramType===active.outputType)||{nodeTypes:[],edgeTypes:[]};
 const {graphRevision,graph}=await api(`/api/v1/diagram/graph/get/${active.id}`);
 const storyMap=active.outputType==='STORY_MAP';
 // The graph only has node IDs; Creativity edit/delete endpoints need the entity IDs
 const entityIds=new Map();
 if(storyMap){const found=await api(`/api/v1/creativity/search/${active.id}?type=ALL`);for(const r of (Array.isArray(found)?found:[]))entityIds.set(r.nodeId,r.id)}
 const rawNodes=new Map((p.outputs||[]).flatMap(o=>o.nodes||[]).map(n=>[n.id,n]));
 const elements=graph.nodes.map(n=>toElement(n,active.id,storyMap,entityIds.get(n.id),rawNodes.get(n.id)));
 const idByKey=new Map(graph.nodes.map(n=>[n.key,n.id]));
 const relations=graph.edges.map(e=>({id:e.id,source:idByKey.get(e.sourceKey),target:idByKey.get(e.targetKey),type:e.type,label:e.label,outputId:active.id,properties:e.properties}));
 return {...p,backendDomain:p.domain,domain,outputs:p.outputs||[],diagrams,activeOutputId:active.id,activeType:active.outputType,graphRevision,storyMap,
  nodeTypes:rules.nodeTypes.map(toUiType),edgeTypes:rules.edgeTypes,
  diagramTypes:types.filter(t=>t.domain===p.domain&&t.diagramType!=='BRANCHING_STORY').map(t=>t.diagramType),
  elements,relations};
}
// ID of the project's diagram of a type, created when missing (branching tab, character balance)
export async function diagramIdFor(project,diagramType){
 if(!API_MODE)return 'demo';
 const known=(project.diagrams||[]).find(d=>d.outputType===diagramType);if(known)return known.id;
 const existing=(await listDiagrams(project.id)).find(d=>d.outputType===diagramType);
 const diagram=existing||await createDiagram(project.id,project.name,diagramType);
 project.diagrams=[...(project.diagrams||[]),diagram];
 return diagram.id;
}

// ---------- API writes ----------
const revision=p=>`?expectedRevision=${p.graphRevision}`;
async function writeApiNode(projectId,type,values,oldId){
 const p=await getApiProject(projectId),existing=oldId?p.elements.find(e=>String(e.id)===String(oldId)):null;
 const outputId=p.activeOutputId;
 const x=Number(values.x??existing?.x??140),y=Number(values.y??existing?.y??135);
 if(p.storyMap){
   if(!CREATIVE_TYPES.includes(type))throw new Error('Story maps hold characters, worlds, locations, rules and events only');
   const payload={positionX:x,positionY:y,...(type==='events'?{title:values.name}:{name:values.name}),description:values.description||'—',notes:values.notes||''};
   // Keep advanced text fields already stored by the backend when editing.
   for(const field of ['aliases','appearance','traits','strengths','weaknesses','skills','history','goals','fears','innerConflict','secrets','characterValues','characterArc','lore','atmosphere','scope','conditions','exceptions','consequences','inWorldTime'])if(existing?.raw?.[field]!=null)payload[field]=existing.raw[field];
   if(type==='characters'){payload.shortDescription=values.description||'—';payload.role=values.role||existing?.role||'OTHER';payload.age=Number(values.age)||Number(existing?.age)||18;payload.writerNotes=values.notes||'';}
   if(type==='locations'){payload.locationType=values.locationType||'';payload.narrativeSignificance=values.description||'';}
   if(type==='events'){payload.eventType=values.eventType||'';payload.presentationOrder=Number(values.presentationOrder)||1;payload.chronologicalOrder=values.chronologicalOrder?Number(values.chronologicalOrder):null;}
   await api(`/api/v1/${entityPaths[type]}/${existing?`update/${existing.entityId}`:`add/${outputId}`}`,json(existing?'PUT':'POST',payload));
 }else{
   // Keep properties this form does not show (for example ERD attributes or AI-added fields)
   const data={...(existing?.properties||{})};delete data.label;
   data.description=values.description||'';if(values.notes)data.notes=values.notes;else delete data.notes;
   const backendType=toBackendType(type);
   if(backendType==='ENTITY'&&!Array.isArray(data.attributes))data.attributes=[{name:'id',dataType:'INT',primaryKey:true,nullable:false}];
   const body={type:backendType,label:values.name,positionX:x,positionY:y,data};
   await api(existing?`/api/v1/diagram/node/update/${outputId}/${existing.nodeId}${revision(p)}`:`/api/v1/diagram/node/add/${outputId}${revision(p)}`,json(existing?'PUT':'POST',body));
 }
 return getApiProject(projectId);
}

// ---------- AI (n8n through the backend) ----------
export const aiApi={
 start:(projectId,body)=>api(`/api/projects/${projectId}/ai/requests`,json('POST',body)),
 status:statusUrl=>api(statusUrl),
 proposal:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}`),
 accept:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}/accept`,{method:'POST'}),
 reject:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}/reject`,{method:'POST'}),
};

export const repository={
 async list(){return API_MODE?(await getApiProjects()).map(projectWithCounts):load().map(projectWithCounts)},
 async get(id){return API_MODE?getApiProject(id):load().find(p=>String(p.id)===String(id))||null},
 async createProject(payload){
  if(API_MODE){
   const before=await getApiProjects();const domain=payload.domain||'writing';
   await api(`/api/v1/project/add/${USER_ID}`,json('POST',{name:payload.name,description:payload.description||'',domain:backendDomain(domain),status:'DRAFT'}));
   const after=await getApiProjects();const ids=new Set(before.map(x=>x.id));const p=after.find(x=>!ids.has(x.id));
   if(!p)throw new Error('Project created but could not resolve its ID; refresh projects before retrying');
   storage.set(uiDomainKey(p.id),domain);
   const diagram=await createDiagram(p.id,payload.name,PRIMARY_DIAGRAM[domain]);storage.set(activeDiagramKey(p.id),diagram.id);
   for(const node of starterNodes(payload.templateId,payload.lang))await writeApiNode(p.id,node.type,node);
   return {...p,domain};
  }
  const projects=load(),p={id:makeId(),name:payload.name,description:payload.description||'',domain:payload.domain||'writing',templateId:payload.templateId||null,status:'DRAFT',createdAt:now(),updatedAt:now(),elements:starterNodes(payload.templateId,payload.lang).map(n=>({...n,id:makeId()})),relations:[]};persist([p,...projects]);return p;
 },
 async updateProject(id,patch){
  // The domain cannot change after creation, so the stored backend domain is sent back as it is
  if(API_MODE){const old=await api(`/api/v1/project/get/${id}`);await api(`/api/v1/project/update/${id}`,json('PUT',{name:patch.name??old.name,description:patch.description??old.description,domain:old.domain,status:patch.status??old.status,version:old.version}));return {...old,...patch,domain:toUiDomain(old)};}
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
  if(API_MODE){
   const p=await getApiProject(projectId);
   if(p.storyMap)await api(`/api/v1/${entityPaths[element.type]}/delete/${element.entityId}`,{method:'DELETE'});
   else await api(`/api/v1/diagram/node/delete/${p.activeOutputId}/${element.nodeId}${revision(p)}`,{method:'DELETE'});
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.elements=p.elements.filter(e=>String(e.id)!==String(element.id));p.relations=p.relations.filter(r=>r.source!==element.id&&r.target!==element.id);p.updatedAt=now();persist(all);return p;
 },
 async moveNode(projectId,id,x,y){
  if(API_MODE){
   const p=await getApiProject(projectId),e=p.elements.find(e=>String(e.id)===String(id));if(!e)throw new Error('Node not found');
   const px=Math.round(x),py=Math.round(y);
   // Story map nodes are plain ProjectNodes (the entity row holds the fields); other diagrams use the revision-checked endpoint
   if(p.storyMap){if(!e.rawNode)throw new Error('Node coordinates unavailable');await api(`/api/v1/node/update/${e.nodeId}`,json('PUT',{type:e.rawNode.type,dataJson:e.rawNode.dataJson||null,positionX:px,positionY:py}));return;}
   const data={...e.properties};delete data.label;
   await api(`/api/v1/diagram/node/update/${p.activeOutputId}/${e.nodeId}${revision(p)}`,json('PUT',{type:toBackendType(e.type),label:e.name,positionX:px,positionY:py,data}));return;
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));const e=p.elements.find(e=>String(e.id)===String(id));if(e){e.x=x;e.y=y;persist(all)}
 },
 async allowedRelationTypes(projectId,sourceId,targetId){
  if(!API_MODE)return ['CUSTOM','FAMILY','FRIENDSHIP','RIVALRY','WORK','ROMANTIC','PARTICIPATES_IN','CAUSES','AFFECTED_BY','LIVES_IN','BELONGS_TO','VISITS','LEADS_TO','PRECEDES','SIMULTANEOUS_WITH','NARRATIVE_LINK','INSIDE','CONNECTED_TO','NEAR','APPLIES_TO','RESTRICTS','AFFECTS'];
  const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(sourceId)),b=p.elements.find(e=>String(e.id)===String(targetId));
  if(!a||!b)return p.edgeTypes;
  if(!p.storyMap)return p.edgeTypes;
  const types=await api(`/api/v1/creativity/relations/get/types/${p.activeOutputId}/${a.nodeId}/${b.nodeId}`);
  return Array.isArray(types)?types.map(t=>typeof t==='string'?t:t.name||String(t)):['CUSTOM'];
 },
 async createRelation(projectId,data){
  if(API_MODE){
   const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(data.source)),b=p.elements.find(e=>String(e.id)===String(data.target));
   if(!a||!b)throw new Error('Both nodes must be on the open diagram');
   const type=data.type||p.edgeTypes[0];
   if(p.storyMap){const label=data.label||(type==='CUSTOM'?'Related':type.replaceAll('_',' '));await api(`/api/v1/creativity/relations/add/${p.activeOutputId}/${a.nodeId}/${b.nodeId}`,json('POST',{type,label}));}
   else await api(`/api/v1/diagram/edge/add/${p.activeOutputId}${revision(p)}`,json('POST',{type,label:data.label||'',sourceNodeId:a.nodeId,targetNodeId:b.nodeId,data:{}}));
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));if(!p)throw new Error('Project not found');p.relations.push({id:makeId(),source:Number(data.source),target:Number(data.target),type:data.type||'CUSTOM',label:data.label||'Related'});p.updatedAt=now();persist(all);return p;
 },
 async removeRelation(projectId,r){
  if(API_MODE){
   const p=await getApiProject(projectId);
   if(p.storyMap)await api(`/api/v1/edge/delete/${r.id}`,{method:'DELETE'});
   else await api(`/api/v1/diagram/edge/delete/${p.activeOutputId}/${r.id}${revision(p)}`,{method:'DELETE'});
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.relations=p.relations.filter(x=>String(x.id)!==String(r.id));p.updatedAt=now();persist(all);return p;
 },
 // Show another diagram of the project on the map
 async setActiveDiagram(projectId,outputId){storage.set(activeDiagramKey(projectId),outputId);return getApiProject(projectId)},
 // Add a new empty diagram to the project and show it
 async addDiagram(projectId,title,diagramType){const d=await createDiagram(projectId,title,diagramType);storage.set(activeDiagramKey(projectId),d.id);return getApiProject(projectId)},
 async importProject(data){if(API_MODE)throw new Error('Import available in demo only');if(!data||typeof data.name!=='string'||!Array.isArray(data.elements)||!Array.isArray(data.relations))throw new Error('Invalid JSON');const p=clone(data),ids=new Map();p.id=makeId();p.elements=p.elements.map(e=>{const id=makeId();ids.set(e.id,id);return {...e,id}});p.relations=p.relations.filter(r=>ids.has(r.source)&&ids.has(r.target)).map(r=>({...r,id:makeId(),source:ids.get(r.source),target:ids.get(r.target)}));p.updatedAt=now();persist([p,...load()]);return p}
};
