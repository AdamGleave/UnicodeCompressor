/* JavaScript driver for my HTML based slide presentations. */
/* See slides.css for accompanying style definitions. */
/* This code is based on an idea by Marijn Haverbeke. */
/* $Id: slides.js,v 1.130 2016/03/31 16:03:34 chris Exp $ */
/* Author: Christian Steinruecken */

var page = 0;
    pages = undefined,
    maxPage = 0,
    jump = "",
    find = "",
    cues = {},
    vmode = 'full', /* view mode */
    pmode = 'full', /* previous view mode */
    show_tb = '1';  /* show toolbar */
var localdocpath='../crh/';
var activeBubble = undefined;
var dpc = undefined;   /* default page class */
var indexwindow = null;
/* Version information */
var sl_js_version = '$Id: slides.js,v 1.130 2016/03/31 16:03:34 chris Exp $';
var sl_css_version = '(unknown)';
var sl_html_version = '(unknown)';
/* Detect if webpage is on local file system, or on a webserver. */
var localmode = (window.location.protocol == 'file:');
/* Detect view mode (full, flow or grid) */
if (document.body.getAttribute('view') != undefined) {
  vmode = document.body.getAttribute('view');
}
/* Detect presentation language (or use language of browser) */
if (document.body.getAttribute('lang') != undefined) {
  lang = document.body.getAttribute('lang');
} else {
  lang = window.navigator.userLanguage || window.navigator.language;
  if (lang != undefined) {
    document.body.setAttribute('lang',lang);
    markAutoAttr(document.body,"lang");
  }
}
/* Check body attribute "tb": show toolbar */
if (document.body.getAttribute('tb') != undefined) {
  show_tb = document.body.getAttribute('tb');
}
/* Check body attribute "dpc": default page class */
if (document.body.getAttribute('dpc') != undefined) {
  dpc = document.body.getAttribute('dpc');
}
/* Detect if HTML file contains optional annotations. */
var annotations = (document.body.getElementsByTagName("ann").length > 0);
/* Message log function. */
function logmsg(msg) {
  var lmsg = document.createElement("div");
  lmsg.innerHTML = msg;
  logbook = document.getElementById('syslog');
  if (logbook != undefined) {
    logbook.insertBefore(lmsg,logbook.childNodes[0]);
  } else {
    //document.body.appendChild(lmsg);
  }
}
/* An array rotation function. */
Array.prototype.rotate = function(n) {
  this.unshift.apply(this, this.splice(n, this.length))
  return this;
}
/* Create a box for displaying error messages. */
function createErrorBar() {
  var errbox = document.createElement('div');
  errbox.setAttribute('id','message');
  errbox.setAttribute('class','error');
  errbox.setAttribute('style','position: fixed; bottom: 0.1ex; font-size: 80%; margin: 4px; padding: 3px; visibility: hidden; z-index: 50;');
  markAutoAttr(errbox,'@');
  document.body.appendChild(errbox);
}
/* Clipboard for copying and pasting slides. */
var clipnode = null;
function copyPage(n) {
  var node = pages[n];
  if (node != undefined) {
    var descr = getDescription(node);
    if (descr !== "") { descr = " ("+descr+")"; }
    clipnode = node.cloneNode(true);
    clipnode.removeAttribute("id"); /* remove id */
    deactivatePage(clipnode); // de-activate
    logmsg("Copied slide <em>"+n+"</em>"+descr);
    setMessage("Copied slide <em>"+n+"</em>"+descr);
  } else {
    setMessage("Copying failed.");
    logmsg("Copying failed: slide <em>"+n+"</em> not found.");
  }
}
function cutPage(n) {
  var node = pages[n];
  if (node != undefined) {
    var descr = getDescription(node);
    if (descr !== "") { descr = " ("+descr+")"; }
    if (n == page) { deactivatePage(node); } // de-activate
    clipnode = node.cloneNode(true); // then clone
    var pc = node.parentNode;
    pc.parentNode.removeChild(pc);
    clipnode.removeAttribute("id"); /* remove id */
    logmsg("Cut slide <em>"+n+"</em>"+descr+".");
    setMessage("Cut slide <em>"+n+"</em>"+descr+".");
    var pp = page;
    // Repair the slide index
    initPages();
    // Correct the current view
    if (pp > maxPage && pp > 0) {
      activatePage(pages[pp-1]);
      forceChangePage(pp-1);
    } else {
      activatePage(pages[pp]);
    }
    updatePageIndicator();
  } else {
    setMessage("Cutting failed.");
    logmsg("Cutting failed: slide <em>"+n+"</em> not found.");
  }
}
function pastePageAfter(n) {
  if (pages[n] != undefined) {
    var node = pages[n].parentNode; // get page container
    if (clipnode != undefined) {
      var descr = getDescription(clipnode);
      if (descr !== "") {
        descr = " ("+descr+")";
      }
      if (clipnode.nodeName.toUpperCase() === 'PAGE') {
        descr = "slide"+descr;
      } else {
        descr = clipnode.nodeName+"-node"+descr;
      }
      var pp = page;
      insertPageAfter(clipnode.cloneNode(true),node);
      logmsg("Pasted "+descr+" after slide <em>"+n+"</em>.");
      setMessage("Pasted "+descr+" after slide <em>"+n+"</em>.");
      initPages(); // repair page numbers, keys, etc.
      // Correct the current view
      //activatePage(pages[n]);
      forceChangePage(n);
      updatePageIndicator();
    } else {
      setMessage("Pasting failed: empty clipboard.");
      logmsg("Pasting failed: empty clipboard.");
    }
  } else {
    setMessage("Pasting failed: invalid destination.");
    logmsg("Pasting failed: empty clipboard.");
  }
}
/* A function that generates toolbar buttons. */
function buttoncode(unicode,ascii,tooltip,code) {
  var buttoncode='<button onclick="'+code+'"';
  buttoncode+=' title="'+tooltip+'"';
  // Android seems to have bad unicode support
  /*
  if (navigator.userAgent.match(/Android/i)) {
    buttoncode+='>'+ascii+'</button>';
  } else {
  */
    buttoncode+='>'+unicode+'</button>';
  //}
  return buttoncode;
}
/* Create an overlay bubble for showing source code. */
function createSourceBubble() {
  var sb = document.createElement('bubble');
  sb.id="sourcecode";
  sb.setAttribute('class','black sourcewindow screenonly');
  sb.setAttribute('descr','Source Code');
  markAutoAttr(sb,'@');
  document.body.appendChild(sb);
}
/* Create an overlay bubble with helpful instructions. */
function createHelpBubble() {
  var hb = document.createElement('bubble');
  hb.id="help";
  hb.setAttribute('class','white helpwindow screenonly');
  hb.setAttribute('descr','Help');
  markAutoAttr(hb,':');
  var inner = "\n";
  inner+='<!-- This HTML code is autogenerated by slides.js -->\n';
  inner+='<span class="app_action app_close" style="float:right; font-size: 70%;" onclick="toggleBubble('+"'help'"+');"></span>';
  inner+='<h3><span id="hlb_help"></span></h3>';
  inner+='<fieldset><legend style="font-size: 90%;"><hl id="hlb_kbdcntrl"></hl></legend>';
  inner+='<table class="kbd_table" style="font-size: 70%;" cellspacing="1" cellpadding="1">';
  inner+='<tr><td><key><span id="hlb_key_up"></span></key>, ';
  inner+='<key><span id="hlb_key_pgup"></span></key></td>';
  inner+='    <td><span id="hlb_cmd_prev"></span></td></tr>';
  inner+='<tr><td><key><span id="hlb_key_dn"></span></key>, ';
  inner+='<key><span id="hlb_key_pgdn"></span></key></td>';
  inner+='    <td><span id="hlb_cmd_next"></span></td></tr>';
  inner+='<tr><td><key><span id="hlb_key_home"></span></key></td>';
  inner+='    <td><span id="hlb_cmd_first"></span></td></tr>';
  inner+='<tr><td><key><span id="hlb_key_end"></span></key></td>';
  inner+='    <td><span id="hlb_cmd_last"></span></td></tr>';
  inner+='<tr><td>[0-9]* <key title="enter">⏎</key></td>';
  inner+='    <td><span id="hlb_cmd_jumpn"></span></td></tr>';
  inner+='<tr><td>[A-Z]* <key title="enter">⏎</key></td>';
  inner+='    <td><span id="hlb_cmd_jumpk"></span></td></tr>';
  inner+='<tr><td><key>.</key></td>';
  inner+='    <td><span id="hlb_cmd_tgtb"></span></td></tr>';
  inner+='<tr><td><key>,</key></td>';
  inner+='    <td><span id="hlb_cmd_tgvm"></span></td></tr>';
  inner+='<tr><td><key>\\</key></td>';
  inner+='    <td><span id="hlb_cmd_tgcol"></span></td></tr>';
  inner+='<tr><td><key>\'</key></td>';
  inner+='    <td><span id="hlb_cmd_src"></span></td></tr>';
//  <tr><td><key title="twiddle">~</key></td><td>Toggle annotations</td></tr>
  inner+='</table>';
  inner+='</fieldset>';
  hb.innerHTML = inner;
  document.body.appendChild(hb);
}
/* Create an overlay bubble for showing source code. */
function createVersionBubble() {
  var sb = document.createElement('bubble');
  sb.id="version";
  sb.setAttribute('class','black versionwindow screenonly');
  sb.setAttribute('descr','Version Info');
  markAutoAttr(sb,':');
  // HTML version (according to meta header)
  var metas = document.getElementsByTagName('meta');
  var sl_html_version="(unknown)";
  for (var i = 0; i < metas.length; i++) {
    if (metas[i].name.toLowerCase() == "version") {
      sl_html_version = metas[i].content;
    }
  }
  // CSS style sheet version
  var rules = document.styleSheets.item(0);
  try {
    rules = rules.cssRules || rules.rules;
    if (rules != undefined) {
      for (var i = 0; i < rules.length; i++) {
        if (rules.item(0).selectorText == '#sl_css_ver') {
          sl_css_version = rules.item(i).style.content;
          sl_css_version = sl_css_version.substring(1,sl_css_version.length-1);
          break;
        }
      }
    } else {
      sl_css_version = "(unknown: not found)";
    }
  }
  catch (e) {
    logmsg("Inspecting CSS rules triggered an exception: <dim>"+e+"</dim>");
    sl_css_version = "(unknown: exception)";
  }
  // mathjax version
  if (typeof(MathJax) != "undefined") {
    mathjax_version = MathJax.fileversion;
  } else {
    mathjax_version = "(not loaded)";
  }
  // access mode
  var accmode = (localmode ? "LOCAL" : "ONLINE");
  // client version
  if (navigator && navigator.platform && navigator.language) {
    client_ver = navigator.platform+' ('+navigator.language+') ';
  } else {
    client_ver = '(unknown)';
  }
  // screen resolution
  if (window.screen) {
    screen_info = window.screen.width+' × '+window.screen.height+'';
  } else {
    screen_info = '(unknown)';
  }
  // generate HTML
  var inner = "\n";
  inner+='<!-- This HTML code is autogenerated by slides.js -->\n';
  inner+='<span class="app_action app_close" style="float:right; font-size: 70%;" onclick="toggleBubble('+"'version'"+');"></span>';
  inner+='<h2>Version Info</h2>\n';
  inner+='<table style="color: inherit;"><tbody>\n';
  inner+='<tr><td align="right" valign="top"><hl>HTML</hl></td>';
  inner+='    <td><tt id="sl_html_ver" title="The header\'s version META-tag">'
       + sl_html_version+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>JavaScript</hl></td>\n';
  inner+='    <td><tt id="sl_js_ver">'+sl_js_version+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>CSS</hl></td>\n';
  inner+='    <td><tt id="sl_css_ver">'+sl_css_version+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>MathJax</hl></td>\n';
  inner+='    <td><tt id="mathjax_ver" onclick="document.getElementById(\'mathjax_ver\').innerHTML=MathJax.fileversion;">'+mathjax_version+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>Access</hl></td>\n';
  inner+='    <td valign="top"><tt id="access_ind">'+accmode+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>Screen</hl></td>\n';
  inner+='    <td><tt id="screen_info">'+screen_info+'</tt></td></tr>\n';
  inner+='<tr><td align="right" valign="top"><hl>Client OS</hl></td>\n';
  inner+='    <td><tt id="client_ver">'+client_ver+'</tt></td></tr>\n';
  inner+='</tbody></table>\n';
  sb.innerHTML = inner;
  document.body.appendChild(sb);
}
/* Get version info and write it to predefined HTML ids. */
// Deprecated old method -- to be removed entirely.
function versionInfo() { }
/* Create a toolbar with buttons for navigation and settings. */
function createToolBar() {
  var tbar = document.createElement('div');
  tbar.setAttribute('id','toolbar');
  tbar.setAttribute('class','toolbar screenonly');
  if (show_tb == "1") {
    tbar.setAttribute('style','position: fixed; bottom: 0; width: 100%; margin: 0px; visibility: visible;');
  } else {
    tbar.setAttribute('style','position: fixed; bottom: 0; width: 100%; margin: 0px; visibility: hidden;');
  }
  markAutoAttr(tbar,'@');
  // add buttons
  var tbcode = "";
  // UP symbols: ↑,⇧ (Arrows)  ⬆ (MSA)  ▲,△ (Geo)  ⬆ (Emoji)
  tbcode+=buttoncode('▲','Up','Backwards','if (event.shiftKey) { changePage(0); } else { changePage(page-1); }');
  // DN symbols: ↓,⇩ (Arrows)  ⬇ (MSA)  ▼,▽ (Geo)  ⬇ (Emoji)
  tbcode+=buttoncode('▼','Dn','Forwards','if (event.shiftKey) { changePage(maxPage); } else { changePage(page+1); }');
  // COL symbols: ⬔ (MSA)  ◩,◪,▣,■ (Geo)
  tbcode+=buttoncode('◩','Col','Toggle colours','if (event.shiftKey) { toggleAllPageClasses(); } else { togglePageClass(); }');
  if (annotations) {
    // ANN symbols: i (Latin) ℹ (Letterlike) ✎,❗ (Dingbats)
    tbcode+=buttoncode('@','Ann','Toggle annotations','toggleAnnotations();');
  }
  // CON symbols: ⚠,⚡ (Miscsym) ◬ (Geo), ⛔
  tbcode+=buttoncode('◬','Sys','Debugging console','toggleSysLog();');
  tbcode+=buttoncode('≡','Scr','Toggle presentation mode','if (event.shiftKey) { toggleIndexWindow(); } else { toggleViewMode(); }'); // ☉
  /*
  if (maxPage > 10) {
    tbcode+=buttoncode('◳','Idx','Toggle slide index window','toggleIndexWindow();');
  }
  */
  tbcode+=" ";
  // add page indicator
  tbcode+='<span id="page_ind" class="page_ind"> </span>';
  // access mode indicator
  //tbcode+='<span id="access_ind" style="font-size: 70%; padding-left: 1ex; padding-right: 1ex; opacity: 0.85;" title="Access mode" class="lu"> </span>';
  // keyword indicator
  tbcode+='<span id="key_ind" class="key_ind"> </span>';
  // "close toolbar" button [x]
  tbcode+='<span class="act" style="float:right; margin-right: 1ex; cursor: default;" onclick="toggleToolbar();" title="Hide toolbar">×</span>';
  // END OF TOOLBAR
  tbar.innerHTML = tbcode;
  document.body.appendChild(tbar);
}
/* Create a system console for logging debugging output. */
function createSyslog() {
  var syslog = document.createElement('div');
  syslog.setAttribute('id','syslog');
  //syslog.setAttribute('style','position: fixed; top: 0.1ex; font-size: 50%; margin: 4px; padding: 2px; visibility: hidden; z-index: 25; max-height: 8em; width: 25%; overflow: auto; background-color: #002200; color: #00ff00; opacity: 0.7; resize: both;');
  syslog.setAttribute('id','syslog');
  var slcode='DEBUG CONSOLE [<a onclick="toggleSysLog();">Hide</a>] [<a onclick="destroy(\'syslog\');">Destroy</a>]';
  syslog.innerHTML=slcode;
  markAutoAttr(syslog,'@');
  document.body.appendChild(syslog);
}
/* Create a timer clock. */
function createTimer() {
  var timer = document.createElement('div');
  timer.id = "timer";
  timer.setAttribute('ondblclick',"resetTimer();");
  timer.setAttribute('onclick',"toggleTimer();");
  timer.style.visibility = "hidden";
  timer.innerHTML='<span id="clockface" title="Timer" class="timer">00:00</span>';
  markAutoAttr(timer,'@');
  document.body.appendChild(timer);
  logmsg("Created timer widget.");
}
/* Script tracking */
var tags_requested = 0;
var tags_loaded = 0;
/* Script / style loading function */
function loadtags(srctag,tag) {
  var srctags = document.getElementsByTagName(srctag);
  logmsg("Loading &lt;"+srctag+"&gt;: "+srctags.length+" "+tag
         +" tag"+(srctags.length!=1 ? "s." : "."));
  for (var j = 0; j < srctags.length; j++) {
    tags_requested++;
    var newtag = document.createElement(tag);
    markAutoAttr(newtag,'@');
    newtag.setAttribute('async','false');
    newtag.setAttribute('defer','false');
    newtag.innerHTML = srctags[j].innerHTML;  // copy the content
    newtag.addEventListener('load',
       function (e) {
         tags_loaded++;
         if (tags_loaded == tags_requested) {
           logmsg("All &lt;"+tag+"&gt;-tags finished loading.");
           //if (tag.toUpperCase() === 'SCRIPT') {
           logmsg("Triggering slide setup.");
           setupPages();
           //}
         }
       }, false);
    if (srctags[j].attributes != null) {
      // copy over all script attributes
      for (var a=0; a<srctags[j].attributes.length; a++) {
        //logmsg("Setting a["+a+"]:"+srctags[i].attributes[a].nodeName);
        newtag.setAttribute(srctags[j].attributes[a].nodeName,
                            srctags[j].attributes[a].value);
      }
    }
    var src = newtag.attributes.getNamedItem("src");
    if (src != undefined) {
      logmsg("Loading: <em>"+src.value+"</em>");
    } else {
      logmsg("Inserting "+tag+" tag without src-attribute.");
    }
    srctags[j].parentNode.insertBefore(newtag,srctags[j]);
  }
}
function loadscripts(scrptag) { loadtags(scrptag,"script"); }
function loadstyles(styletag) { loadtags(styletag,"style"); }
/* A function for registering auto-added attributes. */
function markAutoAttr(n,a) {
  var auto = n.getAttribute('_auto');
  a='_auto '+a;
  if (auto == undefined) {
    n.setAttribute('_auto',a);
  } else {
    var attrs = a.split(' ');
    var autos = auto.split(' ');
    // remove attributes which are already marked as auto
    for (var k=0; k<autos.length; k++) {
      var i = attrs.indexOf(autos[k]);
      if (i != -1) { attrs.splice(i,1); }
    }
    a = attrs.join(' ');
    n.setAttribute('_auto',auto+' '+a);
  }
}
/* A function that creates a "pagecontainer" wrapper to contain
 * a given slide. */
function makePageContainer(pn) {
  pagewrap = document.createElement("pagecontainer");
  pagewrap.setAttribute('class','pagecontainer');
  markAutoAttr(pagewrap,'=');
  pagewrap.appendChild(pn);
  return pagewrap;
}
/* Number all pages and extract all keys... */
function initPages() {
  logmsg("Numbering + indexing slides...");
  pages = document.body.getElementsByTagName("page");
  maxPage = pages.length - 1;
  cues = {}; /* delete any previously stored contents */
  for (var i = 0; i <= maxPage; i++) {
    pages[i].id = i;
    // to turn off page tracking, disable this line:
    pages[i].setAttribute('onmouseover','trackPage('+i+');');
    markAutoAttr(pages[i],'id onmouseover active');
    var dblclick = pages[i].getAttribute('ondblclick');
    if (dblclick == undefined) {
      //pages[i].setAttribute('ondblclick','togglePageEditor('+i+');');
      pages[i].setAttribute('ondblclick','pageDoubleClick('+i+');');
      markAutoAttr(pages[i],'ondblclick');
    }
    // apply default page class, if needed + requested
    var pageclass = pages[i].getAttribute('class');
    if (pageclass == undefined && dpc != undefined) {
      pages[i].setAttribute('class',dpc);
      //markAutoAttr(pages[i],'class');
    }
    // create an outer wrapper (if none exists yet)
    if (!(pages[i].parentNode.nodeName.toUpperCase() === 'PAGECONTAINER')) {
      pagewrap = makePageContainer(pages[i].cloneNode(true));
      pages[i].parentNode.replaceChild(pagewrap,pages[i]);
      pages[i] = pagewrap.firstChild;
    }
    // register keys and add anchors
    cue = pages[i].attributes.getNamedItem("key");
    if (cue != undefined) {
      var kws = cue.value.toLowerCase();
      var kw  = kws.split(' ');
      for (var k=0; k<kw.length; k++) {
        if (cues[kw[k]] != undefined) {
          logmsg("<em>Warning</em>: <a href=\"#"+i+"\">slide "+i+"</a> "
                +"duplicates key '"+kw[k]+"' (also used by <a href=\"#"+cues[kw[k]]+"\">slide "+cues[kw[k]]+"</a>)");
        }
        cues[kw[k]] = i;
        // insert an anchor just before the page, for jumping
        anchor = document.createElement("a");
        anchor.setAttribute('name',kw[k]);
        pages[i].parentNode.insertBefore(anchor,pages[i]);
        // FIXME: duplicate anchors are created (for all pages)
        // when initPages() is executed multiple times.
        // It's a non-critical bug with no known side effects,
        // but nonetheless a bug.
      }
    }
  }
  if (indexwindow != undefined && !indexwindow.closed) {
    updateIndexWindow();
  }
  logmsg("Numbering + indexing complete.");
  logmsg('[<a onclick="setupPages();">Click to run slide setup.</a>]');
}
/* Run initialisation code for each page... */
function setupPages() {
  logmsg("Slide-specific setup running...");
  pages = document.body.getElementsByTagName("page");
  maxPage = pages.length - 1;
  for (var i = 0; i <= maxPage; i++) {
    setup = pages[i].attributes.getNamedItem("setup");
    if (setup != undefined) {
      logmsg('<a href="#'+i+'">Slide '+i+'</a>: executing setup: "'+setup.value+'"');
      res = eval(setup.value);
    }
  }
  if (indexwindow != undefined && !indexwindow.closed) {
    updateIndexWindow();
  }
  logmsg("Slide-specific setup complete.");
}
/* Create system console, if missing. */
if (document.getElementById("syslog") == undefined) {
  createSyslog();
}
/* Create error bar */
if (document.getElementById("message") == undefined) {
  createErrorBar();
}
/* Load scripts depending on mode of access (local vs remote). */
var lscripts = null;
if (localmode) {
  logmsg("Starting in LOCAL ACCESS mode.");
  loadtags("localstyle","style");
  loadtags("localscript","script");
  var forceload_style = "loadtags('remotestyle','style');";
  var forceload_script = "loadtags('remotescript','script');";
  logmsg('[<a onclick="'+forceload_script+'">Click'
        +' to force-load remote scripts.</a>]');
  logmsg('[<a onclick="'+forceload_style+'">Click'
        +' to force-load remote styles.</a>]');
} else {
  logmsg("Starting in REMOTE ACCESS mode.");
  loadtags("remotestyle","style");
  loadtags("remotescript","script");
}
/* Initialize all pages. */
initPages();
/* Setup all pages now if no scripts are loading. */
if (tags_requested == 0) {
  logmsg("No scripts waiting to load: Triggering slide setup.");
  setupPages();
}
/* Create tool bar */
if (document.getElementById("toolbar") == undefined) {
  createToolBar();
}
/* Create source bubble */
if (document.getElementById("sourcecode") == undefined) {
  createSourceBubble();
}
/* Create timer widget */
if (document.getElementById("timer") == undefined) {
  createTimer();
}
/* Process all bubbles (fixed popup elements). */
var bubbles = document.body.getElementsByTagName("bubble");
for (var i = 0; i < bubbles.length; i++) {
  var ct = bubbles[i].attributes.getNamedItem("id").value;
}
var citations = document.body.getElementsByTagName("citation");
/* Process all entries in the bibliography. */
var citations = document.body.getElementsByTagName("citation");
var citemark = {};
for (var i = 0; i < citations.length; i++) {
  var ct = citations[i].attributes.getNamedItem("id").value;
  var cturl = citations[i].attributes.getNamedItem("url");
  if (cturl != undefined) { cturl = cturl.value; }
  var ctfnm = citations[i].attributes.getNamedItem("lfnm");
  if (ctfnm != undefined) {
    ctfnm = ctfnm.value;
  } else {
    ctfnm = ct+'.pdf';
  }
  citations[i].setAttribute('number',i+1);
  markAutoAttr(citations[i],"number");
  citemark[ct]=i+1;
  var links="";
  if (localmode) {
    links=' <a class="localfile screenonly" href="'+localdocpath
             +ctfnm+'" target="_blank" _auto="@">Open ►</a>'; // ▲ ► ▶
  }
  if (cturl != undefined) {
    links+=' <a class="remotefile screenonly" href="'+cturl+'" _auto="@">Get</a>';
  }
  citations[i].innerHTML = '<span class="citekey" _auto="@">'+citemark[ct]+'</span>'
                         + '<span class="citationtext" _auto="=">'
                         + citations[i].innerHTML + links + '</span>';
}
/* Find and activate all citations. */
var cites = document.body.getElementsByTagName("cite");
for (var i = 0; i < cites.length; i++) {
  var aref = cites[i].attributes.getNamedItem("ref");
  if (aref != undefined) {
    var ref = aref.value;
    cites[i].setAttribute('onclick','toggleBubble("'+ref+'","Citation");');
    markAutoAttr(cites[i],'onclick');
    var cm = citemark[ref];
    //cites[i].appendChild(num);
    cites[i].innerHTML+='<sup class="citemark" _auto="@">'+cm+'</sup>';
  }
}
/* Function library... */
function isNumber(n) {
  return !isNaN(parseFloat(n)) && isFinite(n);
}
/* Session settings */
var sconfig = [];
function dropSetting(name) {
  var j = sconfig.indexOf(name);
  if (j != -1) {
    sconfig.splice(j,1);
    logmsg("Dropping <em>"+name+"</em> from session config.");
  }
}
function applySetting(v,act) {
  var remember = false;
  if (v.length == 2) {
    if (v[0]=='view') {
      if (v[1] != vmode && act) {
        setViewMode(v[1]);
      }
      remember = true;
    } else
    if (v[0]=='lang') {
      if (v[1] != lang && act) {
        lang=v[1];
        setLang(v[1]);
      }
      remember = true;
    } else
    if (v[0]=='iw') {
      var j = (indexwindow != undefined && !indexwindow.closed) ? '1' : '0';
      if (v[1] != j && act) {
        toggleIndexWindow();
      }
      remember = true;
    } else
    if (v[0]=='tb') {
      var j = (isVisible('toolbar') ? '1' : '0');
      if (v[1] != j && act) {
        toggle('toolbar');
      }
      remember = true;
    } else
    if (v[0]=='dc') {
      var j = (isVisible('syslog') ? '1' : '0');
      if (v[1] != j && act) {
        toggle('syslog');
      }
      remember = true;
    } else
    if (v[0]=='drop' && act) {
      dropSetting(v[1]);
    }
  }
  return remember;
}
function stickySetting(name) {
  sticky = [ 'view', 'iw', 'tb', 'dc', 'lang' ];
  return (sticky.indexOf(name) != -1);
}
function dropSettings() {
  sconfig = [];
  logmsg("Dropping session config.");
}
function applySettings(vs,act) {
  var nconfig = [];
  for (var k=0; k<vs.length; k++) {
    v = vs[k].split('=');
    var keep = stickySetting(v[0]);
    if (keep) {
      if (sconfig.indexOf(v[0]) == -1) {
        sconfig.splice(sconfig.length,0,v[0]);
      }
      nconfig.splice(nconfig.length,0,v[0]);
    }
  }
  // sync sconfig and nconfig
  for (var k=0; k<sconfig.length; k++) {
    if (nconfig.indexOf(sconfig[k]) == -1) {
      dropSetting(sconfig[k]);
    }
  }
  // apply settings
  for (var k=0; k<vs.length; k++) {
    v = vs[k].split('=');
    applySetting(v,act);
  }
  return true;
}
/* Current user URL config settings */
var uconf = undefined;
function applyConfig(h,act) {
  if (h != undefined && h.indexOf('?') != -1) {
    hs = h.split('?');
    hs.splice(0,1);
    uconf = h;
    return applySettings(hs,act);
  } else {
    dropSettings();
    uconf = undefined;
    return false;
  }
}
function getConfig() {
  var config = "";
  for (var i=0; i<sconfig.length; i++) {
    switch (sconfig[i]) {
      case "view":
         config += '?view='+vmode; break;
      case "lang":
         config += '?lang='+lang; break;
      case "iw":
         config=config+'?iw='+((indexwindow != undefined && !indexwindow.closed) ? '1' : '0');
         break;
      case "tb":
         config=config+'?tb='+(isVisible('toolbar') ? '1' : '0');
         break;
      case "dc":
         config=config+'?dc='+(isVisible('syslog') ? '1' : '0');
         break;
      default:
         logmsg("Unknown session parameter: "+sconfig[i]);
         config += '?'+sconfig[i];
         break;
    }
  }
  return config;
}
/* Location management */
function getLocation() {
  var l = window.location.hash.slice(1);
  if (l.indexOf('?') != -1) {
    return l.split('?')[0];
  } else {
    return l;
  }
}
function hashChange(h) {
  /* try to detect new hash location */
  var loc = undefined;
  var set = undefined;
  var sid = h.indexOf('?');
  if (sid != -1) {
    loc = h.slice(1,sid);
    set = h.slice(sid);
  } else {
    loc = h.slice(1); // redundant?
  }
  var action = 0;
  if (set != uconf) {
    logmsg("URL config change: <em>"+(set != undefined ? set : '')+"</em>");
    applyConfig(set,true);
    action = 1;
  }
  if (loc != page && loc != '') {
    // NOTE: "loc == page" can happen with page tracking
    logmsg("URL location change: <em>"+loc+"</em>");
    // try keyword first...
    if (cues[loc.toLowerCase()] != undefined) {
      jumpToKey(loc);
    } else {
      // otherwise, try jumping by number
      var newpage = h ? Number(loc) : page;
      if (isNumber(newpage)) {
        changePage(newpage);
      } else {
        // check if there's a matching bubble
        // FIXME: should this be cached, like cues[]?
        for (var i = 0; i < bubbles.length; i++) {
          var ct = bubbles[i].attributes.getNamedItem("id").value;
          if (loc == ct) {
            showBubble(loc);
            return;
          }
        }
        // otherwise, give up
        logmsg("Undefined location: <em>"+loc+"</em>");
        setMessage("No such location: <em>"+loc+"</em>");
      }
    }
    action = 1;
  }
  /* if (action == 0) { logmsg("URL change (no action taken)."); } */
}
function deactivatePage(n) {
  if (n != undefined) {
    n.setAttribute("active","");
    if (indexwindow != undefined && !indexwindow.closed) {
      indexwindow.deactivate(n.id);
    } else {
      indexwindow = undefined;
    }
  }
}
function activatePage(n) {
  if (n != undefined) {
    n.setAttribute("active","yes");
    if (indexwindow != undefined && !indexwindow.closed) {
      indexwindow.activate(n.id);
    } else {
      indexwindow = undefined;
    }
  }
}
function pageDoubleClick(n) {
  /* Toggles between "presentation mode" and "choose slide mode" */
  if (vmode == "full" || vmode == "flow" || vmode == "2up") {
    if (pmode != vmode) {
      var tmp = vmode;
      setViewMode(pmode);
      pmode = tmp;
    } else {
      pmode = vmode;
      setViewMode("grid");
    }
  } else {
    if (vmode != pmode) {
      var tmp = vmode;
      setViewMode(pmode);
      pmode = tmp;
    } else {
      setViewMode("full");
    }
  }
}
function jumpToKey(kk) {
  var k = kk.toLowerCase(); // key
  newpage = cues[k];
  if (newpage != undefined) {
    logmsg("Jump to key: <em>"+k+"</em> (page "+newpage+")");
    deactivatePage(pages[page]);
    //setMessage("Loading: <em>"+k+" ("+pp.id+")</em>");
    // change the ID field before jumping
    // -- this trick fixes an alignment issue in Firefox
    pages[newpage].id = k;
    document.location.hash = k;
    pages[newpage].id = newpage;
    document.location.hash = k + getConfig();
    page = newpage;
    activatePage(pages[page]);
    setIndicator(page+" / "+maxPage);
  } else {
    logmsg("Jump to key: <em>"+k+"</em> (undefined)");
    setMessage("No such key: <em>"+k+"</em>");
  }
  updatePageIndicator();
}
function jumpTo(s) {
  var ss = s.toLowerCase(); // key
  var tbub = document.getElementById(ss);
  var tkey = cues[ss];
  if (tkey != undefined) {
    // try jumping by key
    jumpToKey(ss);
  } else
  if (tbub != undefined) {
    toggleBubble(ss);
  } else {
    logmsg("Jump to <em>"+ss+"</em>: no such target.");
    setMessage("Unknown target: <em>"+s+"</em>");
  }
}
function changePage(n) {
  //logmsg("Jump to page: <em>"+n+"</em> (from "+page+")");
  // deactivate current page
  deactivatePage(pages[page]);
  page = Math.max(Math.min(n, maxPage), 0);
  document.location.hash = "#" + page;
  find=""; jump="";
  activatePage(pages[page]);
  updatePageIndicator();
  document.location.hash = "#" + page;
  document.location.hash = "#" + page + getConfig();
}
function forceChangePage(n) {
  if (isNumber(n)) {
    deactivatePage(pages[page]);
    logmsg("Force jump to page: <em>"+n+"</em>");
    page = Math.max(Math.min(n-1, maxPage), 0);
    document.location.hash = "#" + page;
    page = Math.max(Math.min(n, maxPage), 0);
    //document.location.hash = "#" + page;
    document.location.hash = "#" + page;
    document.location.hash = "#" + page + getConfig();
    find=""; jump="";
    activatePage(pages[page]);
  } else {
    // try jumping by keyword
    deactivatePage(pages[page]);
    logmsg("Force jump to key: <em>"+n+"</em>");
    var k = n.toLowerCase(); // key
    newpage = cues[k];
    if (newpage != undefined) {
      document.location.hash = "#" + newpage;
      pages[newpage].id = k;
      document.location.hash = k + getConfig();
      pages[newpage].id = newpage;
      page = newpage;
    } else {
      // try finding a matching bubble...
      for (var i = 0; i < bubbles.length; i++) {
        var ct = bubbles[i].attributes.getNamedItem("id").value;
        if (n == ct) {
          showBubble(ct);
          return;
        }
      }
      logmsg("Jump to key: <em>"+k+"</em> (no such page or bubble)");
      setMessage("No such key: <em>"+k+"</em>");
    }
    activatePage(pages[page]);
  }
  updatePageIndicator();
}
function trackPage(n) {
  if (isNumber(n)) {
    if (page != n) {
      if (page == -1) {
        logmsg("Quietly tracking page to: <em>"+n+"</em>");
        page = n;
        activatePage(pages[page]);
      } else {
        logmsg("Tracking page to: <em>"+n+"</em> (from "+page+")");
        deactivatePage(pages[page]);
        page = n;
        pages[n].id = "tmp";
        window.location.hash = n + getConfig();
        pages[n].id = n;
        activatePage(pages[page]);
      }
      find=""; jump="";
      updatePageIndicator();
    }
  }
}
/* Find a page node by string (key or id). */
function getPageNode(p) {
  var k = p.toLowerCase();
  var thepage = cues[k];
  if (thepage != undefined) {
    return pages[thepage];
  } else
  if (isNumber(p) && p >= 0) {
    thepage = Math.max(Math.min(p, maxPage), 0);
    return pages[thepage];
  } else {
    return undefined;
  }
}
/* Find the a given node's parent page node. */
function trackNode(node) {
  var nm = (node != undefined) ? node.nodeName : "undefined";
  while (node != undefined && node.nodeName != "PAGE") {
    node = node.parentNode;
  }
  if (node != undefined) {
    trackPage(node.id);
    return false;
  } else {
    logmsg("failed to find page of node: "+nm);
    return true;
  }
}
function updatePageIndicator() {
  setIndicator(page+" / "+maxPage);
  // print current key
  cue = pages[page].attributes.getNamedItem("key");
  if (cue != undefined) {
    var ki = document.getElementById('key_ind');
    if (ki != undefined) {
      var lcues = cue.value.split(' ');
      var s = '';
      for (var k=0; k<lcues.length; k++) {
        s += "<a href=\"#"+lcues[k]+"\">"+lcues[k]+"</a> ";
      }
      ki.innerHTML=s;
    }
  } else {
    var ki = document.getElementById('key_ind');
    if (ki != undefined) { ki.innerHTML=''; }
  }
}
function setIndicator(s) {
  var pi = document.getElementById('page_ind');
  if (pi != undefined) {
    pi.innerHTML=s;
  }
}
function setMessage(s) {
  msgobj = document.getElementById("message");
  msgobj.innerHTML = s;
  msgobj.style.visibility = 'visible';
  setTimeout("clearMessage();",1000);
}
function clearMessage() {
  msgobj.style.visibility = 'hidden';
}
var pageclasstoggle = "white black aqua hgrey";
var pageclassdefault = undefined;
function togglePageClass() {
  var tg = (pages[page].getAttribute('classtoggle'));
  if (tg == undefined) {
    if (pageclasstoggle != undefined) {
      tg = pageclasstoggle;
    }
  }
  if (tg != null) {
    // use global class toggle list
    var tgs = tg.split(" ");
    var pcs = (pages[page].getAttribute('class'));
    if (pcs == undefined) { pcs = "white"; }
    var pc = pcs.split(" "); // a page can have many classes
    if (pc.length > 0) { // use first class
      var cn = tgs.indexOf(pc[0]);
      if (cn != -1) {
        // if current class is in the toggle list: cycle forward
        if (cn < tgs.length-1) {
          pc[0] = tgs[cn+1];
        } else {
          pc[0] = tgs[0];
        }
        pages[page].setAttribute('class',pc.join(" "));
      } else {
        // might not be safe to proceed, since we can't know if pc[0]
        // is a safely toggleable class.
        logmsg("Toggling class <em>"+pc[0]+"</em> might not be safe.");
        // pages[page].setAttribute('class',tgs[0]+" "+pcs);
        // pages[page].setAttribute('classtoggle',tgs.join(" ")+cc);
      }
    } else {
      // no class: add one, starting from the second toggle item
      pages[page].setAttribute('class',tgs[tgs.length > 1 ? 1 : 0]);
    }
  } else {
    if (pages[page].getAttribute('class') == "black") {
      pages[page].setAttribute('class',"white");
    } else {
      pages[page].setAttribute('class',"black");
    }
  }
  notifyChange(pages[page]);
}
function setPageClass(p,cid) {
  // get current classes of page p
  var pcs = (p.getAttribute('class'));
  if (pcs != undefined) {
    var pc = pcs.split(" "); // a page can have many classes
    if (pc.indexOf(cid) != -1) {
      // nothing to do, class cid is already active
      return;  
    } else {
      // otherwise: unset current class
      var tgs = (p.getAttribute('classtoggle'));
      if (tgs == undefined) {
        if (pageclasstoggle != undefined) {
          tgs = pageclasstoggle;
        }
      }
      var tg = tgs.split(" ");
      for (var i=0; i<pc.length; i++) {
        for (var j=0; j<tg.length; j++) {
          if (pc[i] == tg[j]) {
            // class found in toggle list: update page and return
            pc[i] = cid;
            p.setAttribute('class',pc.join(" "));
            if (tg.indexOf(cid) == -1) {
              tgs = (p.getAttribute('classtoggle'));
              if (tgs != undefined) {
                p.setAttribute('classtoggle',tgs+" "+cid);
              }
            }
            return;
          }
        }
      }
      // none of class's components was found in the toggle list
      // so simply prepend the new class
      logmsg("cid: "+cid+", pcs: "+pcs);
      p.setAttribute('class',cid+" "+pcs);
      // and make sure it's in the toggle list
      if (tg.indexOf(cid) == -1) {
        tgs = p.getAttribute('classtoggle');
        if (tgs != undefined) {
          p.setAttribute('classtoggle',tgs+" "+cid);
        }
      }
    }
  } else {
    // no class: so let's add one.
    p.setAttribute('class',cid);
    // add it to class toggle, too, if required
    tgs = p.getAttribute('classtoggle');
    if (tgs != undefined) {
      p.setAttribute('classtoggle',tgs+" "+cid);
    }
  }
}
function setAllPageClasses(cid) {
  if (pageclasstoggle != undefined) {
    /* add the new scheme to pageclasstoggle, so that subsequent
       classtoggle operations can detect that the class is
       togglable. */
    if (pageclasstoggle.indexOf(cid) == -1) {
      pageclasstoggle+=" "+cid;
    }
  }
  for (var i=0; i <= maxPage; i++) {
    setPageClass(pages[i],cid);
  }
}
function toggleAllPageClasses() {
  if (pageclasstoggle != undefined) {
    var tgs = pageclasstoggle.split(" ");
    if (tgs.length > 1) {
      setAllPageClasses(tgs[0]);
      tgs.rotate(1);
      pageclasstoggle=tgs.join(" ");
    }
  }
}
function replacePageClasses(src,trg) {
  for (var i=0; i <= maxPage; i++) {
    var pcs = (pages[i].getAttribute('class'));
    if (pcs != undefined) {
      var pc = pcs.split(" "); // a page can have many classes
      if (pc.indexOf(src) != -1) {
        // page has src class: replace with trg class
        setPageClass(pages[i],trg);
      } else {
        // nothing to do
      }
    } else {
      // no action
    }
  }
}
function toggleAnnotations() {
  anns = document.body.getElementsByTagName("ann");
  if (anns[0].style.visibility == 'hidden') {
    for (var a=0; a < anns.length; a++) {
      anns[a].style.visibility='visible';
    }
  } else {
    for (var a=0; a < anns.length; a++) {
      anns[a].style.visibility='hidden';
    }
  }
}
function isVisible(id) {
  var n = document.getElementById(id);
  if (n != undefined) {
    return (n.style.visibility!='hidden');
  } else {
    return false;
  }
}
function toggle(id) {
  var n = document.getElementById(id);
  if (n != undefined) {
    if (n.style.visibility=='visible') {
      logmsg("Hiding <em>"+id+"</em>");
      n.style.visibility='hidden';
    } else {
      logmsg("Showing <em>"+id+"</em>");
      n.style.visibility='visible';
    }
  } else {
    logmsg("Can't toggle <em>"+id+"</em>: undefined.");
  }
}
function destroy(id) {
  var n = document.getElementById(id);
  if (n != undefined) {
    n.parentNode.removeChild(n);
    logmsg("Destroyed <em>"+id+"</em>.");
  } else {
    logmsg("Can't destroy <em>"+id+"</em>: undefined.");
  }
}
function toggleSysLog() { toggle('syslog'); }
function toggleToolbar() { toggle('toolbar'); }
function hideBubble(b) {
  var node = document.getElementById(b);
  node.style.visibility='hidden';
  if (indexwindow != undefined && !indexwindow.closed) {
    indexwindow.unmark(b);
  } else {
    indexwindow = undefined;
  }
  if (activeBubble == b) {
    activeBubble = undefined;
  }
}
function showBubble(b) {
  var bn = "Bubble";
  if (arguments.length > 1) {
    bn = arguments[1];
  }
  if (activeBubble != b) {
    if (activeBubble != undefined) {
      hideBubble(activeBubble);
      activeBubble = undefined;
    }
    var node = document.getElementById(b);
    if (node != undefined) {
      node.style.visibility='visible';
      if (!node.hasAttribute('onclick')) {
        node.setAttribute('onclick','hideBubble("'+b+'");');
        markAutoAttr(node,'onclick');
      }
      if (indexwindow != undefined && !indexwindow.closed) {
        indexwindow.mark(b);
      } else {
        indexwindow = undefined;
      }
      activeBubble = b;
    } else {
      setMessage(bn+" not found: <em>"+b+"</em>");
    }
  }
  logmsg("Active bubble: <em>"+activeBubble+"</em>");
}
function toggleBubble(b) {
  if (activeBubble != b) {
    if (arguments.length > 1) {
      showBubble(b,arguments[1]);
    } else {
      showBubble(b);
    }
  } else {
    hideBubble(b);
    activeBubble = undefined;
    logmsg("No active bubble.");
  }
}
/* Find CSS style rule. */
function getBodyCSS(medium) {
  var sheets = document.styleSheets;
  if (sheets != undefined && typeof(sheets.length) != 'undefined') {
    for (var h = 0; h < sheets.length; h++) {
      var rules = document.styleSheets.item(h);
      rules = rules.cssRules || rules.rules;
      if (rules != undefined) {
        //logmsg(rules.length+" CSS rules.");
        for (var i = 0; i < rules.length; i++) {
          if (rules.item(i).type == CSSRule.MEDIA_RULE) {
            logmsg(i+": @MEDIA rule found: "+rules.item(i).media[0]);
            if (rules.item(i).media[0] == medium) {
              logmsg("Found "+medium+".");
              var ru = rules.item(i).cssRules;
              for (var j=0; j < ru.length; j++) {
                if (ru[j].selectorText == "body") {
                  logmsg("Found BODY.");
                  var fs = ru[j].style.fontSize;
                  logmsg("Style: "+fs);
                  return ru[j].style;
                }
              }
            }
          } else {
            //logmsg(rules.item(i).cssText);
          }
        }
      }
    }
  } else {
    logmsg("getBodyCSS: CSS rules not found.");
  }
}
/* Dynamic page creation and insertion. */
function createPage() {
  return document.createElement("page");
}
function insertAfter(newnode,node) {
  node.parentNode.insertBefore(newnode,node.nextSibling);
}
function insertPageAfter(newpage,node) {
  if (node != undefined) {
    if (node.nodeName.toUpperCase() === 'PAGE') {
      node = node.parentNode; // get page container
    }
    // node is now a pagecontainer or something else
    if (newpage.nodeName.toUpperCase() === 'PAGECONTAINER') {
      // good to go
    } else
    if (newpage.nodeName.toUpperCase() === 'PAGE') {
      // create a pagecontainer
      newpage = makePageContainer(newpage);
    } else {
      logmsg("Warning: inserting a non-page ("+newpage.nodeName+")...");
    }
    insertAfter(newpage,node);
  } else {
    logmsg("Page insertion failed: undefined location.");
  }
}
/* Page removal */
function removePage(n) {
  if (pages[n] != undefined) {
    var descr = getDescription(pages[n]);
    logmsg("Removing page <em>"+n+"</em> ("+descr+").");
    deactivatePage(pages[n]);
    // find the container of the page, and remove it
    var pc = pages[n].parentNode; // page container
    if (pc.nodeName.toUpperCase() === 'PAGECONTAINER') {
      pc.parentNode.removeChild(pc);
    } else {
      logmsg("Warning: Pagecontainer not found. Removing page only.");
      pages[n].parentNode.removeChild(pages[n]);
    }
    initPages();
    if (n > maxPage && n > 0) {
      activatePage(pages[n-1]);
      forceChangePage(n-1);
    } else {
      activatePage(pages[n]);
    }
    updatePageIndicator();
  } else {
    logmsg("Remove page failed: no such page.");
  }
}
/* MathJax removal */
var MJremove = [ 'MathJax', 'MathJax_Preview', 'MathJax_Display',
                 'MathJax_Error', 'MathJax_SVG', 'MathJax_MathML' ];
/* fetches raw, tidied up HTML source code of a given node */
function getSource(x,esc) {
  var result = "";
  if (x.nodeName != undefined) {
    var name = x.nodeName.toLowerCase();
    if (name == "#text") {
      var value = x.nodeValue.split('\n');
      if (esc) {
        result+=toHTML(value[0]);
        for (var i=1; i<value.length; i++) {
          result+='\n'+toHTML(value[i]);
        }
      } else {
        result+=value[0];
        for (var i=1; i<value.length; i++) {
          result+='\n'+value[i];
        }
      }
    } else
    if (name == "#comment") {
      result += '<!--'+x.data+'-->';
    } else {
      var attr = '';
      var cl = ''; // class
      var tp = ''; // type
      var autos = undefined;
      if (x.attributes != undefined) {
        var auto = x.getAttribute('_auto');
        if (auto != undefined) {
          autos = auto.split(' ');
        }
        for (var a=0; a<x.attributes.length; a++) {
          var an = x.attributes[a].nodeName;
          var av = x.attributes[a].value;
          if (auto != undefined && autos.indexOf(an) != -1) {
            // silently ignore the attribute
          } else {
            attr+=' '+an+'=';
            // now quote the value, choosing either " or ' quotes:
            if (av.indexOf('"') != -1) {
              attr+="'"+av+"'";
            } else {
              attr+='"'+av+'"';
            }
            // ...this still fails when both quotes occur, but come on! :)
            if (an == "class") { cl = av; }
            else if (an == "type") { tp = av; }
          }
        }
      }
      var ignoreOuter = false;
      var ignoreInner = false;
      if (autos != undefined) {
        if (autos.indexOf('@') != -1) {
          ignoreOuter = true;
          ignoreInner = true;
        }
        if (autos.indexOf('=') != -1) {
          ignoreOuter = true;
        }
        if (autos.indexOf(".") != -1) {
          ignoreInner = true;
        }
      }
      if (!ignoreOuter || !ignoreInner) {
        if (name == 'span' && MJremove.indexOf(cl) != -1) {
          // ignore it
        } else
        if (name == 'div' &&
              (   cl=='MathJax_Display' || cl=='MathJax_SVG_Display'
               || cl=='MathJax_MathML')) {
          // ignore it
        } else
        if (name == 'script' && tp == 'math/tex; mode=display') {
          // MathJax: extract original TeX source (display mode)
          result+='\\['+x.innerHTML+'\\]';
        } else
        if (name == 'script' && tp == 'math/tex') {
          // MathJax: extract original TeX source
          result+='\\('+x.innerHTML+'\\)';
        } else {
          if (!ignoreOuter) {
            result+='<'+name+attr+'>';
          }
          var children = x.childNodes;
          var expand = !ignoreInner;
          if (children != undefined && expand) {
            if (name == 'style' || name == 'script') {
              // no character escaping
              for (var c=0; c < children.length; c++) {
                result+=getSource(children[c],false);
              }
            } else {
              // otherwise escape as needed
              for (var c=0; c < children.length; c++) {
                result+=getSource(children[c],esc);
              }
            }
          }
          if (name != "br" && name != "img" && !ignoreOuter) {
            result+='</'+name+'>';
          }
        }
      }
    }
  }
  return result;
}
/* produces syntax highlighted HTML source code of a given node */
/* Arguments: x, raw, suppr_nl, preserve */
function getHLSource(x,raw) {
  // optional argument: suppress the first '\n'?
  var suppr_nl = false;
  var preserve = false;
  if (arguments.length > 2) { suppr_nl = arguments[2]; }
  if (arguments.length > 3) { preserve = arguments[3]; }
  var result = "";
  if (x.nodeName != undefined) {
    var name = x.nodeName.toLowerCase();
    if (name == "#text") {
      // FIXME: text inside <pre> and <style> isn't handled correctly
      var value = x.nodeValue.split('\n');
      if (suppr_nl && value.length > 1 && value[0]=='') {
        value.splice(0,1);
      }
      if (raw) {
        result+='<span class="text">'
              +toHTML(value[0],preserve)+"</span>";
        for (var i=1; i<value.length; i++) {
          result+='<br><span class="text">'
                +toHTML(value[i],preserve)+"</span>";
        }
      } else {
        result+='<span class="text">'
              +toHTML(toHTML(value[0]),preserve)+"</span>";
        for (var i=1; i<value.length; i++) {
          result+='<br><span class="text">'
                +toHTML(toHTML(value[i]),preserve)+"</span>";
        }
      }
    } else
    if (name == "#comment") {
      result+='<span class="cmt">&lt;!--</span>'
            +'<span class="in_cmt">'+toHTML(x.data)+'</span>'
            +'<span class="cmt">--&gt</span>';
    } else {
      var attr = '';
      var cl = ''; // class
      var tp = ''; // type
      var autos = undefined;
      if (x.attributes != undefined) {
        var auto = x.getAttribute('_auto');
        if (auto != undefined) {
          autos = auto.split(' ');
        }
        for (var a=0; a<x.attributes.length; a++) {
          var an = x.attributes[a].nodeName;
          var av = x.attributes[a].value;
          if (auto != undefined && autos.indexOf(an) != -1) {
            // silently ignore the attribute
          } else {
            attr+=' <span class="attr">'+an+'</span>=<span class="value">';
            // now quote the value, choosing either " or ' quotes:
            if (av.indexOf('"') != -1) {
              attr+="'"+av+"'";
            } else {
              attr+='"'+av+'"';
            }
            // ...this still fails when both quotes occur, but come on! :)
            attr+='</span>';
            if (an == "class") { cl = av; }
            else if (an == "type") { tp = av; }
          }
        }
      }
      var ignoreOuter = false;
      var ignoreInner = false;
      if (autos != undefined) {
        if (autos.indexOf('@') != -1) {
          ignoreOuter = true;
          ignoreInner = true;
        }
        if (autos.indexOf('=') != -1) {
          ignoreOuter = true;
        }
        if (autos.indexOf(".") != -1) {
          ignoreInner = true;
        }
      }
      if (!ignoreOuter || !ignoreInner) {
        if (name == 'span' && MJremove.indexOf(cl) != -1) {
          // ignore it
        } else
        if (name == 'div' &&
              (   cl=='MathJax_Display' || cl=='MathJax_SVG_Display'
               || cl=='MathJax_MathML')) {
          // ignore it
        } else
        if (name == 'script' && tp == 'math/tex; mode=display') {
          // MathJax: extract original TeX source (display mode)
          var tex = "";
          var tmp = x.innerHTML.split("\n");
          for (var u=0; u<tmp.length; u++) {
            tex += tmp[u].replace(/( %)(.*)/,
                                  "<span class=\"dim\">$1$2</span>");
            if (u+1 < tmp.length) { tex+="<br>"; }
          }
          result+='<span class="mj_delim">\\[</span>'
                 +'<span class="mj">'+tex+'</span>'
                 +'<span class="mj_delim">\\]</span>';
        } else
        if (name == 'script' && tp == 'math/tex') {
          // MathJax: extract original TeX source
          result+='<span class="mj_delim">\\(</span>'
                 +'<span class="mj">'+x.innerHTML+'</span>'
                 +'<span class="mj_delim">\\)</span>';
        } else {
          // include it
          if (!ignoreOuter) {
            result+='<span class="tag">&lt;<span class="name tag_'+name+'">'
                  +name+'</span>'
                  +attr+"&gt;</span>"
          }
          var expand = !ignoreInner;
          var indent = false;
          if (name=='ul' || name=='table' || name=='tr'
           || name=='ol' || name=='center' || name=='svg' || name=='g'
           || name=='dl' || name=='fieldset') {
            indent = true;
          } else
          if (name=='div' || name=='blockquote') {
            indent = true;
            suppr_nl = true;
          }
          var children = x.childNodes;
          if (children != undefined && expand) {
            result+='<span class="in_'+name+'">'
            var skip = 0;
            if (indent) {
              result+='<div class="indent">';
              if (children.length > 0 && children[0].nodeName == '#text') {
                if (children[0].nodeValue == '\n') {
                  // suppress new line
                  skip += 1;
                } else
                if (children[0].nodeValue.replace(/^\s+/, '').replace(/\s+$/, '') === '') {
                  logmsg("html-src: suppressed white-space only text");
                  skip += 1;
                }
              }
            }
            if (name == 'style' || name == 'script') {
              // do not HTML-escape script source,
              // don't suppress first newline, but preserve white-space
              for (var c=skip; c < children.length; c++) {
                result+=getHLSource(children[c],true,false,true);
              }
            } else
            if (name == 'pre') {
              // preserve white-space
              for (var c=skip; c < children.length; c++) {
                result+=getHLSource(children[c],raw,false,true);
              }
            } else {
              // otherwise, escape as needed
              for (var c=skip; c < children.length; c++) {
                result+=getHLSource(children[c],raw,suppr_nl);
              }
            }
            if (indent) { result+='</div>'; }
            result+='</span>';
          }
          if (name != "br" && name != "img" && !ignoreOuter) {
            result+='<span class="tag">&lt;/<span class="name tag_'+name+'">'
                  +name+'</span>&gt;</span>';
          }
        }
      } // endif (!ignore)
    }
  }
  return result;
}
function toHTML(s) { // convert string to HTML (escape special characters)
  var tmp = document.createElement("div");
  tmp.innerText = tmp.textContent = s;
  s = tmp.innerHTML;
  if (arguments.length > 1 && arguments[1] == true) {
    // preserve whitespace
    while (s.indexOf('  ') > -1) {
      s = s.replace('  ','&nbsp; ');
    }
  }
  return s;
}
/* Pop up a text bubble, showing the source code of a given node. */
var sourcenode = undefined;
function showSourceBubble(n) {
  if (sourcenode == n) {
    toggleBubble('sourcecode');
  } else {
    logmsg("Showing source code...");
    sourcenode=n;
    latexnode=null;
    var sc = document.getElementById('sourcecode');
    sc.innerHTML = '';
    var tt = document.createElement("tt");
    tt.setAttribute("class","source html");
    tt.innerHTML=getHLSource(n,false);
    sc.appendChild(tt);
    var ctrl = document.createElement("div");
    ctrl.id="sourcetoolbar";
    ctrl.innerHTML='<span class="app_action app_gethtml" onclick="exportSource(sourcenode);"></span>';
    ctrl.innerHTML+='<span class="app_action app_close" onclick="toggleBubble('+"'sourcecode'"+');"></span>';
    sc.appendChild(ctrl);
    showBubble('sourcecode');
  }
  /*
  var tt = document.createElement("tt");
  tt.setAttribute("class","source html");
  tt.innerHTML=getHLSource(pages[num]);
  var np = createPage();
  np.appendChild(tt);
  insertPageAfter(np,pages[num]);
  if (vmode != 'full') { forceChangePage(num+1); }
  */
}
function showSource() {
  // optional argument: the element whose source to show
  var e = null;
  if (arguments.length > 0) {
    logmsg("Showing source for supplied element.");
    e = arguments[0];
  }
  if (e == null) {
    if (activeBubble != undefined) {
      if (activeBubble != "sourcecode") {
        e = document.getElementById(activeBubble);
        showSourceBubble(e);
      } else {
        toggleBubble('sourcecode');
      }
    } else {
      showSourceBubble(pages[page]);
    }
    /*
    var x = 500;
    var y = 250;
    logmsg("Getting source for element at point ("+x+","+y+").");
    e = document.elementFromPoint(x,y);
    logmsg("e = "+e.tagName+" ["+e+"]");
    while (e != null && e.tagName != "PAGE" && e.tagName != "BUBBLE") {
      e = e.parentNode;
    }
    if (e != null && e.id == "sourcecode") {
      logmsg("Preventing source bubble recursion.");
      // to prevent recursion
      e = sourcecode;
      toggleBubble('sourcecode');
    } else {
      showSourceBubble(e);
    }
    */
  } else {
    showSourceBubble(e);
  }
}
/* Export a given node's source code to a file. */
function exportSource(n) {
  window.open("data:x-application/external;charset=utf-8,"
              + escape(getSource(n,true)));
}
function exportHTMLSource() {
  var result = '';
  result += getSource(document.head.parentNode,true);
  window.open("data:x-application/external;charset=utf-8,"
              + escape(result));
}
/* Invokes a node's onChange handler. */
function notifyChange(node) {
  var change_handler = node.getAttribute("onChange");
  if (change_handler != undefined) { eval(change_handler); }
  if (sourcenode == node) { sourcenode = undefined; }
  if (indexwindow != undefined && !indexwindow.closed
      && node.tagName.toLowerCase() == 'page') {
    indexwindow.updatePage(node.id);
  }
}
/* Revert all processed MathJax from a node to its source code. */
function revertMathJax(n) {
  for (var k=0; k<MJremove.length; k++) {
    var l = n.getElementsByClassName(MJremove[k]);
    while (l.length > 0) {
      l[0].parentNode.removeChild(l[0]);
    }
  }
  l = n.getElementsByTagName('script');
  var k=0;
  while (l.length > k) {
    var tp = l[k].getAttribute('type');
    if (tp == 'math/tex; mode=display') {
      var mjsrc = document.createTextNode('\\['+l[k].innerHTML+'\\]');
      l[k].parentNode.replaceChild(mjsrc,l[k]);
    } else
    if (tp == 'math/tex') {
      var mjsrc = document.createTextNode('\\('+l[k].innerHTML+'\\)');
      l[k].parentNode.replaceChild(mjsrc,l[k]);
    } else {
      // other script: skip
      k++;
    }
  }
}
/* produces LaTeX code approximating a given node */
function getLaTeXfromHTML(x,esc) {
  var result = "";
  if (x.nodeName != undefined) {
    var name = x.nodeName.toLowerCase();
    if (name == "#text") {
      var value = x.nodeValue.split('\n');
      //result+=toLaTeX(value[0]);
      result+=value[0];
      for (var i=1; i<value.length; i++) {
        //result+='\n'+toLaTeX(value[i]);
        result+='\n'+value[i];
      }
    } else
    if (name == "#comment") {
      //result += '% '+toLaTeX(x.data)+'\n';
      result += '% '+x.data+'\n';
    } else {
      var attr = '';
      var cl = ''; // class
      var tp = ''; // type
      var ref = ''; // ref-field (of citations)
      var autos = undefined;
      if (x.attributes != undefined) {
        var auto = x.getAttribute('_auto');
        if (auto != undefined) {
          autos = auto.split(' ');
        }
        for (var a=0; a<x.attributes.length; a++) {
          var an = x.attributes[a].nodeName;
          var av = x.attributes[a].value;
          if (auto != undefined && autos.indexOf(an) != -1) {
            // silently ignore the attribute
          } else {
            if (an == "class") { cl = av; }
            else if (an == "type") { tp = av; }
            else if (an == "ref") { ref = av; }
          }
        }
      }
      var ignoreOuter = false;
      var ignoreInner = false;
      if (autos != undefined) {
        if (autos.indexOf('@') != -1) {
          ignoreOuter = true;
          ignoreInner = true;
        }
        if (autos.indexOf('=') != -1) {
          ignoreOuter = true;
        }
        if (autos.indexOf(".") != -1) {
          ignoreInner = true;
        }
      }
      if (!ignoreOuter || !ignoreInner) {
        if (name == 'span' && MJremove.indexOf(cl) != -1) {
          // ignore it
        } else
        if (name == 'div' &&
              (   cl=='MathJax_Display' || cl=='MathJax_SVG_Display'
               || cl=='MathJax_MathML')) {
          // ignore it
        } else
        if (name == 'script' && tp == 'math/tex; mode=display') {
          // MathJax: extract original TeX source (display mode)
          result+='\\begin{displaymath}\n'+x.innerHTML+'\n\\end{displaymath}\n';
        } else
        if (name == 'script' && tp == 'math/tex') {
          // MathJax: extract original TeX source
          result+='$'+x.innerHTML+'$';
        } else {
          var children = x.childNodes;
          var expand = !ignoreInner;
          var env = '';
          var pre = '';
          var post = '';
          if (children != undefined && expand) {
            if (name == 'style' || name == 'script') {
              expand=false; // ignore any children
            } else
            if (name == 'table') {
              pre  = '\\begin{tabular}{?}\n';
              post = '\\end{tabular}\n';
            } else
            if (name=='td' || name=='th') { pre=' & '; } else
            if (name=='tr') { post=' \\\\\n'; } else
            if (name=='b') {  pre='\\textbf{'; post='}'; } else
            if (name=='i') {  pre='\\textit{'; post='}'; } else
            if (name=='tt') {  pre='\\texttt{'; post='}'; } else
            if (name=='em') {  pre='\\emph{'; post='}'; } else
            if (name=='hl') {  pre='\\hl{'; post='}'; } else
            if (name=='li') {  pre='\\item{'; post='}'; } else
            if (name=='sub') {  pre='$_{'; post='}$'; } else
            if (name=='sup') {  pre='$^{'; post='}$'; } else
            if (name=='h1' || name=='h2') { pre='\\heading{'; post='} % '+name; } else
            if (name=='span' || name=='tbody' || name=='colgroup'
             || name=='col') {
              pre=''; post=''; // nothing to wrap, just recurse
            } else
            if (name=='br') {
              expand=false; // ignore any children
            } else
            if (name=='img') {
              var imgsrc = x.getAttribute("src");
              if (imgsrc == undefined) { imgsrc=""; }
              pre='\\includegraphics{'+imgsrc;
              post='}';
              expand=false; // ignore any children
            } else
            if (name=='hbr') {
              pre='\\\\\n';
              post='\\medskip\n';
              expand=false; // ignore children
            } else
            if (name=='a') { // hyperlinks
              var href = x.getAttribute("href");
              if (href != undefined) {
                pre  = '\\href{'+href+'}{';
                post = '}';
              } else {
                pre  = '';
                post = '';
              }
            } else
            if (name=='cite') {
              pre='\\citet{'+ref+'}'; post=''; expand=false;
            } else
            if (name=='page') {
              pre  = '\\begin{slide}';
              post = '\\end{slide}\n';
              var key = x.getAttribute("key");
              if (key != undefined) {
                pre+="\\label{sl:"+key+"}\n";
              } else {
                pre+="\n";
              }
            } else
            if (name=='ul') { env='itemize'; } else
            if (name=='ol') { env='enumerate'; } else
            if (name=='div' || name=='center') {
              env = name;
            } else { // unknown tag
              pre ='% \\beginHTML{'+name+'}\n';
              post='% \\endHTML{'+name+'}\n';
            }
            if (env != '') {
              pre ='\\begin{'+env+'}\n';
              post='\\end{'+env+'}\n';
            }
            result+=pre;
            if (expand) {
              for (var c=0; c < children.length; c++) {
                result+=getLaTeXfromHTML(children[c],esc);
              }
            }
            result+=post;
          } else
          if (name == "td") {
            result+='&';
          } else
          if (name == "br") {
            result+='\n\n';
          }
        }
      }
    }
  }
  return result;
}
// Show a LaTeX bubble (for node n) */
var latexnode = null;
function showLaTeXBubble(n) {
  if (latexnode == n) {
    toggleBubble('sourcecode');
  } else {
    logmsg("Showing source code [LaTeX]...");
    latexnode=n;
    sourcenode=null;
    var sc = document.getElementById('sourcecode');
    sc.innerHTML = '';
    var tt = document.createElement("pre");
    tt.setAttribute("class","source html");
    tt.innerHTML=getLaTeXfromHTML(n,false);
    sc.appendChild(tt);
    var ctrl = document.createElement("div");
    ctrl.id="sourcetoolbar";
    //ctrl.innerHTML='<span class="app_action app_gethtml" onclick="exportSource(sourcenode);"></span>';
    ctrl.innerHTML+='<span class="app_action app_close" onclick="toggleBubble('+"'sourcecode'"+');"></span>';
    sc.appendChild(ctrl);
    showBubble('sourcecode');
  }
  /*
  var tt = document.createElement("tt");
  tt.setAttribute("class","source html");
  tt.innerHTML=getHLSource(pages[num]);
  var np = createPage();
  np.appendChild(tt);
  insertPageAfter(np,pages[num]);
  if (vmode != 'full') { forceChangePage(num+1); }
  */
}
/* Minimalistic built-in page editor */
function togglePageEditor(n) {
  if (isNumber(n)) {
    var pageclass = pages[n].getAttribute("class");
    var editstatus = pages[n].getAttribute("contentEditable");
    if (pageclass == undefined) { pageclass = ""; }
    if (editstatus == undefined || editstatus == "false") {
      logmsg("Entering edit mode (page <em>"+n+"</em>).");
      pages[n].setAttribute("contentEditable","true");
      pages[n].setAttribute("class",pageclass+" editmode");
      pages[n].setAttribute('ondblclick','');
      markAutoAttr(pages[n],'contenteditable');
      // revert MathJax output back to source
      if (typeof(MathJax) !== 'undefined') { revertMathJax(pages[n]); }
      // page deletion button
      /*
      var killer = document.createElement("span");
      killer.setAttribute("class","killpage");
      killer.setAttribute("id","kp_"+n);
      killer.setAttribute("contentEditable","false");
      killer.setAttribute("title","Delete this page");
      //killer.setAttribute("onclick","document.body.removeChild(document.getElementById('kp_"+n+"')); removePage('"+n+"');");
      killer.setAttribute("onclick","removePage(this.parentNode.id);");
      killer.innerHTML="×";
      pages[n].insertBefore(killer, pages[n].firstChild);
      */
      // "accept page" button
      var accept = document.createElement("span");
      accept.setAttribute("class","savepage");
      accept.setAttribute("id","sp_"+n);
      accept.setAttribute("contentEditable","false");
      accept.setAttribute("title","Save & leave editor");
      accept.setAttribute("onclick","togglePageEditor(this.parentNode.id); document.getElementById('"+n+"').removeChild(document.getElementById('sp_"+n+"'));");
      accept.innerHTML="✔";
      pages[n].insertBefore(accept, pages[n].firstChild);
      //pages[n].setAttribute("style","border-left: 0.5ex solid red;");
    } else {
      logmsg("Leaving edit mode (page <em>"+n+"</em>).");
      pages[n].setAttribute("contentEditable","false");
      pages[n].setAttribute("class",pageclass.replace(" editmode",""));
      pages[n].setAttribute('ondblclick','pageDoubleClick('+n+');');
      /*
      var killer = pages[n].firstChild;
      if (killer != undefined) {
        killer.parentNode.removeChild(killer);
      }
      */
      var accept = pages[n].firstChild;
      if (accept != undefined) {
        accept.parentNode.removeChild(accept);
      }
      /* Re-typeset all MathJax */
      if (typeof(MathJax) !== 'undefined') {
        MathJax.Hub.Queue(["Typeset",MathJax.Hub,pages[n]]);
      }
      notifyChange(pages[n]);
    }
  } else {
    logmsg("togglePageEditor(<em>"+n+"</em>): no such page.");
  }
}
function inEditMode() {
  if (pages[page] != undefined) {
    var editstatus = pages[page].getAttribute("contentEditable");
    return (editstatus == "true");
  } else {
    return false;
  }
}
/* Update view */
function reprocessMathJax(element) {
  if (typeof(MathJax) !== 'undefined' && element != undefined) {
    //var editstatus = element.getAttribute("contentEditable");
    //if (editstatus != "true") {
    MathJax.Hub.Queue(["Reprocess",MathJax.Hub,element]);
    //}
  }
}
function updateView() {
  // this function is called e.g. in response to a window resize event
  logmsg("Updating page rendering.");
  // reprocess local page
  reprocessMathJax(pages[page]);
  // reprocess all other pages
  // ascending
  for (var k=page+1; k<maxPage; k++) {
    reprocessMathJax(pages[k]);
  }
  // descending
  for (var k=page-1; k>0; k--) {
    reprocessMathJax(pages[k]);
  }
}
/* Function for altering the base font size. */
var screenbodycss = undefined;
function changeFontSize(delta) {
  if (screenbodycss == undefined) {
    screenbodycss = getBodyCSS('screen');
  }
  if (screenbodycss != undefined) {
    var fs = screenbodycss.fontSize;
    fs = fs.replace("%","");
    screenbodycss.fontSize=(fs-delta)+"%";
    forceChangePage(page);
  } else {
    logmsg("Failed to find screen CSS.");
  }
}
/* Toggle language setting */
function setLang(l) {
  var langattr = document.body.getAttribute('lang');
  if (langattr == undefined) {
    markAutoAttr(document.body,'lang');
  }
  document.body.setAttribute('lang',l);
  logmsg("Language changed from '"+langattr+"' to '"+l+"'.");
}
/* Toggle presentation mode */
function setViewMode(mode) {
  var oldloc = getLocation();
  var oldpage = page;
  deactivatePage(pages[page]);
  if (mode == 'full') {
    //allowScrolling(false);
    document.body.style.overflow = 'hidden';
    vmode = 'full';
  } else
  if (mode == 'flow') {
    //allowScrolling(true);
    document.body.style.overflow = 'auto';
    vmode = 'flow';
  } else
  if (mode == '2up') {
    //allowScrolling(true);
    document.body.style.overflow = 'auto';
    vmode = '2up';
  } else
  if (mode == 'grid') {
    //allowScrolling(true);
    document.body.style.overflow = 'auto';
    vmode = 'grid';
  }
  // "wake up" all pages, so they are updating themselves
  for (var i = 0; i <= maxPage; i++) {
    var id = pages[i].id;
    pages[i].setAttribute("id",id+" ");
    pages[i].setAttribute("id",id);
  }
  logmsg("Setting view mode to '<em>"+vmode+"</em>'.");
  if (document.body.getAttribute('view') == undefined) {
    markAutoAttr(document.body,'view');
  }
  document.body.setAttribute('view',vmode);
  if (oldloc != "") { forceChangePage(oldloc); }
  activatePage(pages[page]);
}
function toggleViewMode() {
  if (vmode == 'full') {
    setViewMode('flow');
  } else
  if (vmode == 'flow') {
    setViewMode('2up');
  } else
  if (vmode == '2up') {
    setViewMode('grid');
  } else {
    setViewMode('full');
  }
}
/* Write version info into the document. */
createVersionBubble();
/* Write help info into the document. */
createHelpBubble();
/* Get optional config settings from URL and apply them. */
var ovmode = vmode;
//applyConfig(document.location.hash,false); // first read
applyConfig(document.location.hash,true); // then apply
/* Set correct view mode. */
if (ovmode == vmode) {
  setViewMode(vmode);
}
/* Functions for external slide index window. */
function getDescription(node) {
  // first check if the node has a DESCR attribute
  var descr = undefined;
  if (node != null && node.getAttribute == undefined) {
    return node;
  }
  descr = node.getAttribute('descr');
  if (descr != undefined) {
    return descr;
  } else {
    var tags = ['h1','h2','h3','h4'];
    for (var i=0; i<tags.length; i++) {
      descr = node.getElementsByTagName(tags[i]);
      if (descr != undefined && descr.length > 0) {
        return descr[0].innerHTML;
      }
    }
    if (node.children != undefined) {
      for (var i=0; i<node.children.length; i++) {
        if (node.children[i].nodeName == '#text') {
          logmsg("TEXT NODE");
          return node.children[i].value;
        }
      }
    }
  }
  return null;
}
function updateIndexWindow() {
  var wd = indexwindow.document;
  wd.body.innerHTML='';
  wd.write('<html _auto="@"><head><title>Slide Index</title></head><body>');
  wd.write('<style type="text/css">');
  wd.write('@media print { .noprint { display: none; } }');
  wd.write('.pageref { font-weight: bold; }'
          +'.pageref:hover, .pageref td:hover {'
          +'background-color: infobackground; color: infotext; cursor: pointer; }'
          +'.pagenum { text-align: right; vertical-align: top; background-color: buttonface; color: buttontext; padding-left: 0.5ex; padding-right: 0.5ex; opacity: 0.5; }'
          +'.pageref.active { background-color: highlight; color: highlighttext; }'
          +'.pageref.active:hover, .pageref.active td:hover { background-color: highlight; color: highlighttext; opacity: 1.0; }'
          +'.minibt {background-color: buttonface; color: buttontext; text-align: center; cursor: pointer; box-shadow: 0.3ex 0.3ex 0.1ex buttonshadow; margin-left: 0.5ex; opacity: 0.33; user-select: none; -webkit-user-select: none; -moz-user;select: none; -ms-user-select: none;}'
          +'.minibt:hover { opacity: 1.0; }'
          +'.minibt:active {background-color:infotext; color:infobackground;}'
          +'table { font-size: inherit; }'
          +'dim { opacity: 0.5; }'
          +'</style>');
  wd.write('<div class="noprint" style="position: absolute; top: 0; right: 0; margin: inherit; z-index: 2;">');
  wd.write('<span class="minibt" style="float:right; width: 1.2em;" onclick="adjustFontSize(5); return false;">+</span>');
  wd.write('<span class="minibt" style="float:right; width: 1.2em;" onclick="adjustFontSize(-5); return false;">−</span>');
  wd.write('</div>');
  wd.write('<table>');
  var remove = "<b>×</b>"
  indexwindow.getCaption = function(n) {
    var descr = getDescription(n);
    if (descr == null) { descr=" "; }
    var kk = n.getAttribute('key');
    if (kk != undefined) {
      ks = ' <dim>['+kk+']</dim>'
    } else {
      ks = '';
    }
    return descr+ks;
  }
  // include pages
  for (var k=0; k<=maxPage; k++) {
    var cl = "pageref";
    if (k == page) {
      cl += " active";
    }
    wd.write('<tr class="'+cl+'" id="pi_'+k
              +'" onclick="window.opener.forceChangePage('
              +k+');"><td class="pagenum">'+k+'</td>'
              +'<td>'+indexwindow.getCaption(pages[k])+'</td>'
              +'</tr>');
  }
  wd.write('</table>');
  wd.write('<hr>');
  // include bubbles
  wd.write('<table>');
  for (var k=0; k<indexwindow.opener.bubbles.length; k++) {
    // ???
    var bubble = indexwindow.opener.bubbles.item(k);
    var bid = bubble.id;
    var cl = (activeBubble == bubble) ? "pageref active" : "pageref";
    wd.write('<tr class="'+cl+'" id="bi_'+bid
              +'" onclick="window.opener.toggleBubble(\''
              +bid+'\');"><td class="pagenum">B'+k+'</td>'
              +'<td>'+indexwindow.getCaption(bubble)+' <dim>('+bid+')</dim></td>'
              +'</tr>');
  }
  wd.write('</table>');
  indexwindow.document.body.style.fontSize = '100%';
  indexwindow.adjustFontSize = function(n) {
    var nfs = indexwindow.document.body.style.fontSize.replace("%","");
    indexwindow.document.body.style.fontSize = (parseInt(nfs)+n)+"%";
  };
  wd.writeln('<script type="text/javascript">');
  wd.writeln('var page = '+page+';');
  wd.writeln('function activate(k) {');
  wd.writeln('  var as = document.getElementById("pi_"+k);');
  wd.writeln('  if (as != undefined) { as.setAttribute("class","pageref active"); page = k; }');
  wd.writeln('}');
  wd.writeln('function deactivate(k) {');
  wd.writeln('  var as = document.getElementById("pi_"+page);');
  wd.writeln('  if (as != undefined) {');
  wd.writeln('    as.setAttribute("class","pageref");');
  wd.writeln('    if (k == page) { page = -1; }');
  wd.writeln('  }');
  wd.writeln('}');
  wd.writeln('function mark(bid) {');
  wd.writeln('  var as = document.getElementById("bi_"+bid);');
  wd.writeln('  if (as != undefined) { as.setAttribute("class","pageref active"); }');
  wd.writeln('}');
  wd.writeln('function unmark(bid) {');
  wd.writeln('  var as = document.getElementById("bi_"+bid);');
  wd.writeln('  if (as != undefined) { as.setAttribute("class","pageref"); }');
  wd.writeln('}');
  indexwindow.updatePage = function(k) {
    var as = indexwindow.document.getElementById("pi"+k);
    if (as != undefined) {
      as.children[1].innerHTML = indexwindow.getCaption(k);
      logmsg("Slide Index: updated page <em>"+k+"</em>.");
    }
  };
  wd.write('</script>');
  wd.write('</body></html>');
  // indexwindow ###
  //indexwindow.onkeydown = kbd_handler;
}
function toggleIndexWindow() {
  if (indexwindow != undefined && !indexwindow.closed) {
    indexwindow.close();
    indexwindow = undefined;
    logmsg("Closed <em>slide index</em> window.");
  } else {
    indexwindow = undefined;
    //indexwindow = window.open('','slide_index','width=350,menubar=no,toolbar=no,location=no,scrollbars=yes,status=no,dialog=yes,minimizable=yes');
    indexwindow = window.open('','slide_index','width=350,height=500,scrollbars=yes,location=no,toolbar=no,menubar=no');
    if (indexwindow == undefined) {
      logmsg("Failed to open external window. Pop-up blocker?");
      setMessage("Pop-up was blocked.");
    } else {
      // close index window when main window is closed:
      document.body.setAttribute('onunload','if (indexwindow != null && !indexwindow.closed) { indexwindow.close(); }');
      markAutoAttr(document.body,'onunload');
      logmsg("Updating index window: "+indexwindow);
      updateIndexWindow();
      logmsg("Opened external <em>slide index</em> window.");
    }
    //indexwindow.opener.focus();
  }
}
/* Scroll behaviour */
function preventscroll(e) {
  e.preventDefault();
}
function allowScrolling(permit) {
  /* NOTE: this prevents all use of the mouse scroll wheel in the
     window, which seems undesirable as such.  Use of 'allowScrolling'
     is therefore not recommended. */
  if (permit) {
    if (window.removeEventListener) {
      window.removeEventListener('mousewheel', preventscroll, false);
      window.removeEventListener('DOMMouseScroll', preventscroll, false);
    }
  } else {
    if (window.addEventListener) {
      window.addEventListener('mousewheel', preventscroll, false);
      window.addEventListener('DOMMouseScroll', preventscroll, false);
    }
  }
}
//allowScrolling(false);
/* Android compatibility */
//if (navigator.userAgent.match(/Android/i)) {
//  window.scrollTo(0,1);
//}
/* Touch handling */
window.ontouchstart = tch_starter;
window.ontouchmove = tch_mover;
window.ontouchend = tch_ender;
window.ontouchcancel = tch_canceller;
function tch_canceller(e) {
  logmsg("Touch cancelled (fingers="+e.touches.length+").");
  if (e.touches.length >= 1) {
    trackNode(e.touches[0].target);
  }
}
var t_startx = -1;
var t_starty = -1;
var t_startobj = undefined;
var t_endx = -1;
var t_endy = -1;
function tch_starter(e) {
  logmsg("Touch start (fingers="+e.touches.length+").");
  if (e.touches.length >= 1) {
    if (e.touches.length == 1) {
      t_startobj = e.touches[0].target;
      t_startx = e.touches[0].pageX;
      t_starty = e.touches[0].pageY;
      t_endx = t_startx;
      t_endy = t_starty;
    }
    trackNode(e.touches[e.touches.length-1].target);
  }
}
function tch_mover(e) {
  if (vmode == 'full') {
    if (e.touches.length == 1) {
      var tch = e.touches[0];
      var nod = e.touches[0].target;
      t_endx = tch.pageX;
      t_endy = tch.pageY;
    }
    // prevent default page scrolling
    e.preventDefault();
  } else {
    if (e.touches.length >= 1) {
      trackNode(e.touches[e.touches.length-1].target);
    }
  }
  logmsg("Touch move: "+e.touches.length+" fingers, client ("+tch.clientX+","+tch.clientY+"),"
               + " page ("+tch.pageX+","+tch.pageY+")");
}
function tch_ender(e) {
  logmsg("Touch ended (fingers="+e.touches.length+").");
  // analyse gesture
  if (e.touches.length==0) {
    trackNode(t_startobj);
    var difx = t_endx - t_startx;
    var dify = t_endy - t_starty;
    if (Math.abs(difx) > Math.abs(dify)) {
      if (difx > 40) {
        // right
        changePage(page-1);
      } else if (difx < -40) {
        // left
        changePage(page+1);
      } else {
        logmsg("Tiny horizontal touch ignored.");
      }
    } else {
      if (dify > 40) {
        // swipe down (i.e. go up)
        changePage(page-1);
      } else if (dify < -40) {
        // swipe up (i.e. go down)
        changePage(page+1);
      } else {
        logmsg("Tiny vertical touch ignored.");
      }
    }
    t_startx = -1;
    t_starty = -1;
    t_endx = -1;
    t_endy = -1;
  } else {
    trackNode(e.touches[e.touches.length-1].target);
    if (e.touches.length==2) {
      // 3 simultaneous finger touches, no move = change colour
      if (t_endx == t_startx && t_endy == t_starty) {
        togglePageClass();
        //togglePageEditor(page);
        e.preventDefault();
      }
    } else
    if (e.touches.length==3) {
      if (t_endx == t_startx && t_endy == t_starty) {
        showSource();
        e.preventDefault();
      }
    }
  }
}
/* Timer + clock widget functions. */
var tmode = 0;
var tstart = new Date();
var tduration = new Date(0);
function resetTimer() {
  tstart = new Date();
}
function updateTimer() {
  var now=new Date();
  tduration = new Date(now.getTime() - tstart.getTime());
  var h=tduration.getHours();
  var m=tduration.getMinutes();
  var s=tduration.getSeconds();
  // add a zero in front of numbers<10
  m=tpad(m);
  s=tpad(s);
  if (h == 0) {
    document.getElementById('clockface').innerHTML=m+":"+s;
  } else {
    h=tpad(h);
    document.getElementById('clockface').innerHTML=h+":"+m+":"+s;
  }
}
function runTimer() {
  updateTimer();
  if (tmode == 1) {
    t=setTimeout('runTimer()',250);
  }
}
function startTimer() {
  if (tmode != 1) {
    tstart = new Date((new Date()).getTime() - tduration.getTime());
    tmode = 1;
    runTimer();
  }
}
function stopTimer() {
  tmode = 2;
}
function resetTimer() {
  tduration = new Date(0);
  tstart = new Date();
  updateTimer();
}
function toggleTimer() {
  if (tmode == 1) {
    stopTimer();
  } else {
    startTimer();
  }
}
function tpad(i) {
  if (i<10) { i="0" + i; } return i;
}
/* Command mappings */
var cmdmap = {};
function mapcmd(cmd, eval) { cmdmap[cmd.toUpperCase()]=eval; }
function mapcmds(cmds, eval) {
  for (var j=0; j<cmds.length; j++) { mapcmd(cmds[j],eval); }
}
function unmapcmd(cmd) { cmdmap[cmd]=undefined; }
/* Key mappings */
var kbdmap = {};
function mapkey(k, eval) { kbdmap[k]=eval; }
function mapkeys(kk, eval) {
  for (var j=0; j<kk.length; j++) { mapkey(kk[j],eval); }
}
function unmapkey(k) { kbdmap[k]=undefined; }
var scankey = 0;
/* Main keyboard handler. */
window.onkeydown = kbd_handler;
function kbd_handler(e) {
  var key = e.keyCode;
  // check if active element is editable
  var editable = false;
  var actag = document.activeElement.tagName;
  if (actag == "INPUT" || actag == "TEXTAREA" || actag == "SELECT") {
    editable = true;
  }
  /*
  else {
    var catr = document.activeElement.getAttribute("contenteditable");
    if (catr != undefined) {
      editable = (catr.toUpperCase() == "TRUE");
    }
  }
  */
  if (!editable) {
    if (scankey == -1) {
      scankey = key;
      e.preventDefault();
    } else
    if (inEditMode()) {
      if (key == 27) {
        // exit editor
        togglePageEditor(page);
      } else {
        // feed it through
      }
    } else {
      cmd=kbdmap[key];
      if (cmd == undefined) {
        cmd=kbdmap[String.fromCharCode(key)];
      }
      if (cmd != undefined) {
        var proceed = false;
        eval(cmd);
        if (!proceed) { e.preventDefault(); }
      } else
      // ------------------------------------------------------- //
      if (key >= 48 && key <= 57) {  // Digit
        var digit = key - 48;
        if (jump != "" || find == "") {
          find = "";
          jump = jump+""+digit;
        } else {
          find = find+""+digit;
        }
      } else if (key >= 64 && key <= 90) {  // Latin letter
        jump = "";
        find = find+String.fromCharCode(key);
      } else if (key == 13) {  // enter or return
        if (jump != "") {
          changePage(jump); jump="";
        } else
        if (find != "") {
          cmd=cmdmap[find];
          if (cmd != undefined) {
            var proceed = false;
            eval(cmd);
            if (proceed) { jumpTo(find); }
            find="";
          } else {
            jumpTo(find); find="";
          }
        }
        e.preventDefault();
      } else if (key == 8) {    // backspace
        if (jump == "" && find == "") {
          // if number / keyword buffers are empty...
          if (activeBubble != undefined) {
            hideBubble(activeBubble);
            e.preventDefault();
          } else {
            // step backwards
            changePage(page-1);
            e.preventDefault();
          }
        } else {
          // delete 1 char from number buffer / keyword buffer
          jump=jump.substr(0,jump.length-1);
          find=find.substr(0,find.length-1);
        }
      } else {
        // for development and debugging
        var mod = '';
        if (e.ctrlKey) { mod+="Ctrl "; }
        if (e.altKey) { mod+="Alt "; }
        if (e.shiftKey) { mod+="Shift "; }
        if (e.metaKey) { mod+="Meta "; }
        logmsg("Unmapped KEY: "+mod+key+" ("+keyCodeString(key)+")");
      }
    }
  }
}
function keyCodeString(key) {
  /* This function is hopeless even from the beginning.
     Firstly, because it's not clear which key generated a given
     keycode, and that depends on keyboard layout.
     Secondly, because some keys have different names in different
     locales.  Thirdly, because it's hard to find out either. */
  var keycode = -1;
  if (typeof key == 'number') {
    keycode = key;
  } else
  if (typeof key == 'string') {
    keycode = parseInt(key);
  }
  switch(keycode) {
    case 16: return 'Shift';
    case 17: return 'Ctrl'; // string depends on locale
    case 18: return 'Alt';
    case 37: return '←';
    case 38: return '↑';
    case 39: return '→';
    case 40: return '↓';
    case 13: return '⏎';
    case 27: return 'Esc';
    case 32: return 'Space';
    case 33: return 'PgUp'; // string depends on locale
    case 34: return 'PgDn'; // string depends on locale
    case 35: return 'End';  // string depends on locale
    case 36: return 'Home'; // string depends on locale
    case 145: return 'ScrLck';
    case 188: return ',';  // depends on kbd layout
    case 190: return '.';  // depends on kbd layout
    case 192: return '~';  // depends on kbd layout
    case 220: return '\\'; // depends on kbd layout
    case 222: return '\'';  // depends on kbd layout
    default: return String.fromCharCode(key);
  }
}
/* Add event handler */
var addEvent = function(e, type, eventHandle) {
  if (e == null || e == undefined) return;
  if ( e.addEventListener ) {
      e.addEventListener( type, eventHandle, false );
  } else if ( e.attachEvent ) {
      e.attachEvent( "on" + type, eventHandle );
  } else {
      e["on"+type]=eventHandle;
  }
};
/* Deal with window resize events */
var lastResize;
addEvent(window,"resize",function(e) {
  logmsg("Resizing...");
  // FIXME: requires debouncing -- then call updateView();
  // manual call of "updateView()" is possible from the keyboard with "rejax"
});
/* Default mappings. */
// PgUp, Left
mapkeys([33,37],'if (!e.altKey) {changePage(page-1);} else {proceed=true;}');
// Up key
mapkey(38,'if (vmode=="full") { changePage(page-1); } else { proceed=true; }');
// PgDn, Right
mapkeys([34,39],'if (!e.altKey) {changePage(page+1);} else {proceed=true;}');
// Down key
mapkey(40,'if (vmode=="full") { changePage(page+1); } else { proceed=true; }');
// Space key
mapkey(32,'if (activeBubble != undefined) { hideBubble(activeBubble); } else { changePage(page+1); }');
// Home key
mapkey(36,'changePage(0);');
// End key
mapkey(35,'changePage(maxPage);');
// backslash
mapkey(220,'if (e.shiftKey) { toggleAllPageClasses(); } else { togglePageClass(); }');
// dot
mapkey(190,'toggleToolbar();');
// comma, ScrollLock
mapkeys([188,145], 'toggleViewMode();');
// hash, ascii tilde
mapkey(192,'toggleAnnotations();');
//mapkey(186,'changeFontSize(5);');
//mapkey(186,'if (event.shiftKey) { exportHTMLSource(); } else { exportSource(pages[page]); }');
//mapkey(187,'setAllPageClasses("white");');
// Apostrophe
mapkey(222,'showSource();');
//mapkey(222,'toggleIndexWindow();');
mapcmd('index','toggleIndexWindow();');
mapcmd('debug','toggleSysLog();');
mapcmd('rejax','updateView();');
mapcmd('timer','toggle("timer");');
mapcmd('edit','togglePageEditor(page);');
mapcmd('copy','copyPage(page);');
mapcmd('cut','cutPage(page);');
mapcmd('paste','pastePageAfter(page);');
mapcmd('remove','removePage(page);');

//mapcmd('hilfe','document.getElementById("help").setAttribute("lang","de"); toggleBubble("help");');
//mapcmd('help','document.getElementById("help").setAttribute("lang","en"); toggleBubble("help");');
mapcmd('source','showSource();');
mapcmd('latex','showLaTeXBubble(pages[page]);');

// handle changes to the #-part of the URL
page=-1; // IMPORTANT -- other code relies on it
//hashChange(window.location.hash);

if ("onHashChange" in window) {
  window.onHashChange = function() {
    hashChange(window.location.hash);
  }
} else {
  var storedHash = window.location.hash;
  window.setInterval(function() {
    if (window.location.hash != storedHash) {
      storedHash = window.location.hash;
      hashChange(storedHash);
    }
  }, 200);
}
