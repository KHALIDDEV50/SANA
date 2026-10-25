// All three SANA domains share the same project/canvas contract.
// Templates describe editable starter nodes; they are not AI generations.
export const CREATIVE_TYPES = ['characters','worlds','locations','rules','events'];
export const TECH_TYPES = ['services','databases','apis','tasks','decisions'];
export const BUSINESS_TYPES = ['ideas','customers','value','channels','costs'];
export const TYPES = [...CREATIVE_TYPES,...TECH_TYPES,...BUSINESS_TYPES];
export const typesForDomain = domain => domain === 'technology' ? TECH_TYPES : domain === 'business' ? BUSINESS_TYPES : CREATIVE_TYPES;
export const domainGroup = domain => ['writing','games','creativity'].includes(domain) ? 'creativity' : domain;
export const TEMPLATE_CATALOG = [
  {id:'blank-creative',domain:'writing',group:'creativity',ar:'لوحة إبداعية فارغة',en:'Blank creative canvas',arDesc:'مساحة حرة لبناء الشخصيات والعوالم والأحداث.',enDesc:'A free canvas for characters, worlds and plot.'},
  {id:'novel',domain:'writing',group:'creativity',ar:'مخطط رواية',en:'Novel planner',arDesc:'ابدئي بشخصية رئيسية وعالم وحدث أول.',enDesc:'Start with a protagonist, a world and an opening event.',nodes:[{type:'characters',ar:'الشخصية الرئيسية',en:'Protagonist'},{type:'worlds',ar:'عالم القصة',en:'Story world'},{type:'events',ar:'الحدث الأول',en:'Opening event'}]},
  {id:'game',domain:'games',group:'creativity',ar:'قصة لعبة',en:'Game narrative',arDesc:'أساس لبناء شخصيات اللعبة وبيئتها والأحداث.',enDesc:'Outline your game characters, setting and events.',nodes:[{type:'characters',ar:'بطل اللعبة',en:'Game protagonist'},{type:'worlds',ar:'عالم اللعبة',en:'Game world'},{type:'events',ar:'المهمة الأولى',en:'First quest'}]},
  {id:'blank-tech',domain:'technology',group:'technology',ar:'لوحة تقنية فارغة',en:'Blank technical canvas',arDesc:'خططي للمكونات التقنية بطريقتك.',enDesc:'Design your architecture from scratch.'},
  {id:'architecture',domain:'technology',group:'technology',ar:'معمارية نظام',en:'System architecture',arDesc:'واجهة برمجية وخدمة وقاعدة بيانات مترابطة.',enDesc:'Plan an API, application service and database.',nodes:[{type:'apis',ar:'واجهة API',en:'API gateway'},{type:'services',ar:'الخدمة الأساسية',en:'Core service'},{type:'databases',ar:'قاعدة البيانات',en:'Database'}]},
  {id:'sprint',domain:'technology',group:'technology',ar:'تخطيط تطبيق',en:'Application planner',arDesc:'رتبي متطلبات التنفيذ والقرارات التقنية.',enDesc:'Organize build tasks and technical decisions.',nodes:[{type:'tasks',ar:'تجهيز المشروع',en:'Project setup'},{type:'decisions',ar:'قرار تقني',en:'Technical decision'}]},
  {id:'blank-business',domain:'business',group:'business',ar:'لوحة تجارية فارغة',en:'Blank business canvas',arDesc:'ابدئي بخريطة لفكرتك التجارية.',enDesc:'Explore a business idea on a free canvas.'},
  {id:'business-model',domain:'business',group:'business',ar:'نموذج فكرة تجارية',en:'Business idea canvas',arDesc:'الفكرة، العملاء، القيمة والتكاليف في مكان واحد.',enDesc:'Map the idea, customers, value and costs.',nodes:[{type:'ideas',ar:'فكرة المشروع',en:'Business idea'},{type:'customers',ar:'العملاء المستهدفون',en:'Target customers'},{type:'value',ar:'القيمة المقترحة',en:'Value proposition'},{type:'costs',ar:'التكاليف',en:'Costs'}]},
  {id:'marketing',domain:'business',group:'business',ar:'خطة وصول للعملاء',en:'Customer growth plan',arDesc:'حددي العملاء والقنوات والقيمة المقدمة.',enDesc:'Clarify audience, channels and value.',nodes:[{type:'customers',ar:'شريحة العملاء',en:'Audience'},{type:'channels',ar:'قنوات الوصول',en:'Acquisition channels'},{type:'value',ar:'ما الذي يميزنا؟',en:'Our advantage'}]},
];
export const findTemplate = id => TEMPLATE_CATALOG.find(t => t.id === id);
export const starterNodes = (templateId, lang = 'ar') => (findTemplate(templateId)?.nodes || []).map((n,i)=>({type:n.type,name:lang==='en'?n.en:n.ar,description:'',x:105+(i%3)*268,y:118+Math.floor(i/3)*175}));
