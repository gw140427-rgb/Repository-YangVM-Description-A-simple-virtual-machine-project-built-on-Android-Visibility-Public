const $=id=>document.getElementById(id);
const sample="LOAD A, 5\nLOAD B, 3\nADD A, B\nPRINT A\nHALT";
let code=[],pc=0,A=0,B=0,ram=new Array(256).fill(0),halted=false,output=[],logs=[];
function parse(){return $("program").value.split(/\n/).map(s=>s.split(";")[0].trim()).filter(Boolean).map((line,i)=>({line,parts:line.replace(/,/g," ").trim().split(/\s+/)}))}
function reset(){code=parse();pc=0;A=0;B=0;ram=new Array(256).fill(0);halted=false;output=[];logs=[];render()}
function num(s){if(/^0x[\da-f]+$/i.test(s))return parseInt(s,16);const n=Number(s);if(!Number.isInteger(n))throw Error("숫자가 필요해요: "+s);return n}
function reg(s){if(s==="A")return A;if(s==="B")return B;throw Error("레지스터는 A 또는 B만 가능해요: "+s)}
function setreg(s,v){v=((v%256)+256)%256;if(s==="A")A=v;else if(s==="B")B=v;else throw Error("레지스터는 A 또는 B만 가능해요: "+s)}
function step(){if(halted)return;if(pc<0||pc>=code.length){halted=true;logs.push("프로그램 끝");render();return}
const current=pc, ins=code[pc].parts, op=ins[0].toUpperCase();pc++;
try{switch(op){
case "LOAD":if(ins.length!==3)throw Error("사용법: LOAD A, 숫자");setreg(ins[1].toUpperCase(),num(ins[2]));break;
case "ADD":if(ins.length!==3)throw Error("사용법: ADD A, B");setreg(ins[1].toUpperCase(),reg(ins[1].toUpperCase())+reg(ins[2].toUpperCase()));break;
case "SUB":if(ins.length!==3)throw Error("사용법: SUB A, B");setreg(ins[1].toUpperCase(),reg(ins[1].toUpperCase())-reg(ins[2].toUpperCase()));break;
case "STORE":if(ins.length!==3)throw Error("사용법: STORE 주소, A");{const addr=num(ins[1]);if(addr<0||addr>255)throw Error("주소는 0~255");ram[addr]=reg(ins[2].toUpperCase())}break;
case "PRINT":if(ins.length!==2)throw Error("사용법: PRINT A");output.push(String(reg(ins[1].toUpperCase())));break;
case "JMP":{const dest=num(ins[1]);if(dest<0||dest>=code.length)throw Error("이동 위치가 범위를 벗어났어요");pc=dest}break;
case "JNZ":{const dest=num(ins[2]);if(reg(ins[1].toUpperCase())!==0){if(dest<0||dest>=code.length)throw Error("이동 위치가 범위를 벗어났어요");pc=dest}}break;
case "HALT":halted=true;break;
default:throw Error("모르는 명령어: "+op);
}logs.push("["+current+"] "+code[current].line)}catch(e){halted=true;logs.push("오류: "+e.message)}
render()}
function render(){$("regA").textContent=A;$("regB").textContent=B;$("pc").textContent=pc;$("state").textContent=halted?"정지":"실행 가능";$("output").textContent=output.length?output.join("\n"):"아직 출력이 없어요.";const mem=$("memory");mem.innerHTML="";ram.slice(0,16).forEach((v,i)=>{const d=document.createElement("div");d.className="cell";d.innerHTML="<small>"+i.toString().padStart(2,"0")+"</small>"+v;mem.append(d)});$("log").innerHTML="";(logs.length?logs:["실행 기록이 여기에 쌓여요."]).slice(-30).forEach(s=>{const li=document.createElement("li");li.textContent=s;$("log").append(li)})}
$("step").onclick=step;$("run").onclick=()=>{let n=0;while(!halted&&n++<1000)step();if(n>=1000){halted=true;logs.push("안전 제한: 1000회 실행");render()}};$("restart").onclick=reset;$("resetCode").onclick=()=>{$("program").value=sample;reset()};reset();