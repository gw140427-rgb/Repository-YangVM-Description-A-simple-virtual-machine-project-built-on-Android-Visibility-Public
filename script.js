const KEY="yangvm-machines-v1";
const $=id=>document.getElementById(id);
let machines=[];
try{machines=JSON.parse(localStorage.getItem(KEY)||"[]")}catch{machines=[]}
function save(){localStorage.setItem(KEY,JSON.stringify(machines))}
function render(){
 $("count").textContent=machines.length;
 $("running").textContent=machines.filter(m=>m.running).length;
 $("memory").textContent=machines.reduce((n,m)=>n+Number(m.ram),0)+" GB";
 const root=$("machines");root.innerHTML="";
 if(!machines.length){root.innerHTML='<div class="empty">아직 가상 머신이 없어요.<br>＋ 새 VM 만들기로 첫 머신을 추가하세요.</div>';return}
 machines.forEach(m=>{
  const card=document.createElement("article");card.className="vm";
  const icon=document.createElement("div");icon.className="osicon";icon.textContent=m.os==="Linux"?"🐧":m.os==="Windows"?"▦":"💻";
  const info=document.createElement("div");info.className="vm-info";
  const name=document.createElement("strong");name.textContent=m.name;
  const meta=document.createElement("small");meta.textContent=m.os+" · "+m.ram+" GB RAM";
  info.append(name,meta);
  const pill=document.createElement("span");pill.className="pill"+(m.running?" on":"");pill.textContent=m.running?"실행 중":"꺼짐";
  const actions=document.createElement("div");actions.className="actions";
  const toggle=document.createElement("button");toggle.className="action";toggle.textContent=m.running?"중지":"시작";toggle.onclick=()=>{m.running=!m.running;save();render()};
  const del=document.createElement("button");del.className="action danger";del.textContent="삭제";del.onclick=()=>{if(confirm(m.name+" 프로필을 삭제할까요?")){machines=machines.filter(x=>x.id!==m.id);save();render()}};
  actions.append(toggle,del);card.append(icon,info,pill,actions);root.append(card);
 });
}
$("add").onclick=()=>$("createDialog").showModal();
$("close").onclick=$("cancel").onclick=()=>$("createDialog").close();
$("ram").oninput=e=>$("ramOut").textContent=e.target.value+" GB";
$("createForm").onsubmit=e=>{e.preventDefault();machines.push({id:Date.now(),name:$("vmName").value.trim(),os:$("os").value,ram:Number($("ram").value),running:false});save();render();$("createDialog").close();e.target.reset();$("ram").value=2;$("ramOut").textContent="2 GB"};
render();