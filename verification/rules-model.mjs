// Offline structural/logic check of the ACTUAL rules source. This is NOT the Firebase emulator.
// Run the official emulator suite separately to check Google's compiler, limits and semantics.
import fs from 'node:fs';
import assert from 'node:assert/strict';
const eq=(a,b)=>{try{assert.deepEqual(a,b);return true}catch{return false}};
const difference=(a,b)=>new Set([...a].filter(x=>![...b].some(y=>eq(x,y))));
const fields=x=>new Set(Object.keys(x));
const size=x=>x instanceof Set?x.size:Array.isArray(x)||typeof x==='string'?x.length:Object.keys(x).length;
function method(obj,k,args){
 switch(k){
  case 'size': return size(obj);
  case 'keys': return Object.keys(obj);
  case 'toSet': return new Set(obj);
  case 'get': return Object.hasOwn(obj,args[0])?obj[args[0]]:args[1];
  case 'hasOnly': return difference(new Set(obj),new Set(args[0])).size===0;
  case 'hasAll': return difference(new Set(args[0]),new Set(obj)).size===0;
  case 'removeAll': return obj.filter(x=>!args[0].some(y=>eq(x,y)));
  case 'difference': return difference(obj,args[0]);
  // RE2 spells code-point escapes as \x{...}; JavaScript spells them \u{...}.
  case 'matches': return new RegExp(`^(?:${args[0].replace(/\\x\{([0-9A-Fa-f]+)\}/g, '\\u{$1}')})$`,'u').test(obj);
  case 'diff': return {__diff:[obj,args[0]]};
  case 'affectedKeys': return new Set([...new Set([...Object.keys(obj.__diff[0]),...Object.keys(obj.__diff[1])])].filter(k=>!eq(obj.__diff[0][k],obj.__diff[1][k])));
  default:throw Error('Unsupported method '+k);
 }
}
function tokenize(s){
 s=s.replace(/\/\/[^\n]*/g,'').replace(/(\/databases\/(?:[^()\s]|\$\([^)]*\))+)(?=\))/g,(_,path)=>JSON.stringify(path));
 const re=/\s+|'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"|\d+(?:\.\d+)?|[A-Za-z_][A-Za-z_0-9]*|==|!=|<=|>=|&&|\|\||[{}\[\](),;:.?+*/%<>=!-]/gy;
 const out=[];let p=0,m;
 while(p<s.length){re.lastIndex=p;m=re.exec(s);if(!m)throw Error('Token at '+s.slice(p,p+80));p=re.lastIndex;if(!/^\s/.test(m[0]))out.push(m[0]);}return out;
}
const prec={'||':1,'&&':2,'==':3,'!=':3,'in':4,'is':4,'<':4,'>':4,'<=':4,'>=':4,'+':5,'-':5,'*':6,'/':6,'%':6};
class Parser{
 constructor(t){this.t=t;this.i=0}
 take(t){const v=this.t[this.i++];if(t&&v!==t)throw Error(`Expected ${t}, got ${v}`);return v}
 peek(){return this.t[this.i]}
 expr(min=0){
  let v,t=this.take();
  if(t==='!'||t==='-')v=['unary',t,this.expr(7)];
  else if(t==='('){v=this.expr();this.take(')')}
  else if(t==='['){const items=[];while(this.peek()!==']'){items.push(this.expr());if(this.peek()!==',')break;this.take(',')}this.take(']');v=['list',items]}
  else if(t==='{'){const pairs=[];while(this.peek()!=='}'){const key=this.take();this.take(':');pairs.push([key.slice(1,-1),this.expr()]);if(this.peek()!==',')break;this.take(',')}this.take('}');v=['map',pairs]}
  else if(t.startsWith("'")||t.startsWith('"'))v=['literal',t.startsWith('"')?JSON.parse(t):t.slice(1,-1)];
  else if(/^\d/.test(t))v=['literal',Number(t)];
  else if(['true','false','null'].includes(t))v=['literal',JSON.parse(t)];
  else v=['name',t];
  while(this.i<this.t.length){
   t=this.peek();
   if(t==='.'){this.take();v=['member',v,this.take()];continue}
   if(t==='['){this.take();const index=this.expr();this.take(']');v=['index',v,index];continue}
   if(t==='('){this.take();const args=[];while(this.peek()!==')'){args.push(this.expr());if(this.peek()!==',')break;this.take(',')}this.take(')');v=['call',v,args];continue}
   if(t==='?'&&min===0){this.take();const yes=this.expr();this.take(':');v=['ternary',v,yes,this.expr()];continue}
   if(prec[t]===undefined||prec[t]<min)break;
   this.take();v=['binary',t,v,t==='is'?['literal',this.take()]:this.expr(prec[t]+1)];
  }return v;
 }
}
export class RulesModel{
 constructor(file){
  this.functions=new Map();this.allows={};let scope='root';
  const p=new Parser(tokenize(fs.readFileSync(file,'utf8')));
  while(p.i<p.t.length){
   const t=p.take();
   if(t==='match'){
    const match=[];while(p.peek()!=='{')match.push(p.take());
    if(match.includes('ledger'))scope='ledger';else if(match.includes('events'))scope='events';else if(match.includes('users'))scope='users';else if(match.includes('profiles'))scope='profiles';else if(match.includes('friendRequests'))scope='friendRequests';else if(match.includes('friendCodes'))scope='friendCodes';else scope='root';
   }
   if(t==='function'){
    const name=p.take();p.take('(');const args=[];while(p.peek()!==')'){args.push(p.take());if(p.peek()!==',')break;p.take(',')}p.take(')');p.take('{');const body=[];
    while(p.peek()!=='}'){const kind=p.take();if(kind==='let'){const k=p.take();p.take('=');body.push(['let',k,p.expr()]);p.take(';')}else if(kind==='return'){body.push(['return',p.expr()]);p.take(';')}else throw Error(kind)}p.take('}');this.functions.set(name,{args,body});
   }
   if(t==='allow'){
    const ops=[];while(p.peek()!==':'){const op=p.take();if(op!==',')ops.push(op)}p.take(':');p.take('if');const expr=p.expr();p.take(';');for(const op of ops){(this.allows[scope]??={})[op]??=[];this.allows[scope][op].push(expr)}
   }
  }
 }
 evaluate(node,env){
  this.count++;const [kind,a,b,c]=node;
  if(kind==='literal')return a;
  if(kind==='name'){if(Object.hasOwn(env,a))return env[a];throw Error('Unbound '+a)}
  if(kind==='list')return a.map(x=>this.evaluate(x,env));
  if(kind==='map')return Object.fromEntries(a.map(([k,x])=>[k,this.evaluate(x,env)]));
  if(kind==='member'){const obj=this.evaluate(a,env);if(obj==null||!Object.hasOwn(obj,b))throw Error('Missing field '+b);return obj[b]}
  if(kind==='index'){const obj=this.evaluate(a,env),k=this.evaluate(b,env);if(obj==null||!Object.hasOwn(obj,k))throw Error('Missing index '+k);return obj[k]}
  if(kind==='unary'){const x=this.evaluate(b,env);return a==='!'?!x:-x}
  if(kind==='ternary')return this.evaluate(this.evaluate(a,env)?b:c,env);
  if(kind==='binary'){
   const x=this.evaluate(b,env);
   if(a==='&&')return x&&this.evaluate(c,env);if(a==='||')return x||this.evaluate(c,env);
   const y=this.evaluate(c,env);
   switch(a){case '==':return eq(x,y);case '!=':return !eq(x,y);case '+':return x+y;case '-':return x-y;case '*':return x*y;case '/':return x/y;case '%':return x%y;
    case '<':return x<y;case '>':return x>y;case '<=':return x<=y;case '>=':return x>=y;
    case 'in':return Array.isArray(y)?y.includes(x):y instanceof Set?y.has(x):Object.hasOwn(y,x);
    case 'is':return y==='int'?Number.isInteger(x):y==='map'?x!=null&&typeof x==='object'&&!Array.isArray(x):y==='list'?Array.isArray(x):y==='bool'?typeof x==='boolean':typeof x===y;
   }
  }
  if(kind==='call'){
   const args=b.map(x=>this.evaluate(x,env));
   if(a[0]==='member'){if(a[1][0]==='name'&&a[1][1]==='math'){if(a[2]!=='floor')throw Error('Unsupported math '+a[2]);return Math.floor(args[0])}return method(this.evaluate(a[1],env),a[2],args)}
   const name=a[1];
   if(name==='int')return Math.trunc(Number(args[0]));if(name==='string')return String(args[0]);
   if(['get','getAfter','exists','existsAfter'].includes(name)){
    this.accessCalls++;
    const path=args[0].replace(/\$\(([^)]+)\)/g,(_,expression)=>this.evaluate(new Parser(tokenize(expression)).expr(),env)).replace('/databases/test/documents/','');
    const after=name.endsWith('After');this.reads.add((after?'after:':'before:')+path);
    const data=(after?this.after:this.before)[path];return name.startsWith('exists')?data!==undefined:{data};
   }
   const fn=this.functions.get(name);if(!fn)throw Error('Missing function '+name);const local={...env,...Object.fromEntries(fn.args.map((k,i)=>[k,args[i]]))};
   for(const statement of fn.body){if(statement[0]==='let')local[statement[1]]=this.evaluate(statement[2],local);else return this.evaluate(statement[1],local)}
  }
  throw Error('Unsupported node '+JSON.stringify(node));
 }
 authorize(before,writes,uid,{verbose=false}={}){
  this.before=before;this.after={...before};for(const [path,data]of Object.entries(writes)){if(data===null)delete this.after[path];else this.after[path]=data}
  const reports=[];
  for(const [path,data]of Object.entries(writes)){
   const segments=path.split('/');const scope=segments.includes('ledger')?'ledger':segments[0];const op=data===null?'delete':before[path]===undefined?'create':'update';
   const env={database:'test',uid:segments[1],eventId:segments[1],shard:segments[3],code:segments[1],requestId:segments[1],resource:{data:before[path]},request:{auth:uid?{uid}:null,time:1000,resource:{data}}};
   this.count=0;this.accessCalls=0;this.reads=new Set();let allowed=false,error;
   for(const expr of this.allows[scope]?.[op]??[]){try{allowed ||= Boolean(this.evaluate(expr,env))}catch(e){error=e.message}}
   reports.push({path,allowed,expressions:this.count,accesses:this.reads.size,accessCalls:this.accessCalls,error});
  }
  if(verbose)console.log(reports);return {allowed:reports.every(r=>r.allowed),reports};
 }
}
