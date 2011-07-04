//
// Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.. 
//
var http_request = false;
var error        = false;

function startDemo()
{
   document.getElementById("errmsg0").style.display = 'none';
   getStatus();
}

function getData() 
{
   // branch for native XMLHttpRequest object
   url = '/webapp/getInfo';
   if (window.XMLHttpRequest) 
   {
      http_request = new XMLHttpRequest();
      http_request.onreadystatechange = render;
      http_request.open("GET", url, true);
      http_request.send(null);
   } 
   
   // branch for IE/Windows ActiveX version
   else if (window.ActiveXObject) 
   {
      http_request = new ActiveXObject("Microsoft.XMLHTTP");
      if (http_request) 
      {
         http_request.onreadystatechange = render;
         http_request.open("GET", url, true);
         http_request.send();
      }
   }
}

function getStatus() 
{
   getData();
   if(!error) setTimeout("getStatus()", 500);
}

function populateQueue() 
{
   var url        = '/webapp/addWork'
   var parameters =  "unitsOfWork=" + encodeURI(document.getElementById("unitsOfWork").value);
   http_request   = false;
   if (window.XMLHttpRequest) { // Mozilla, Safari,...
      http_request = new XMLHttpRequest();
      if (http_request.overrideMimeType) 
         http_request.overrideMimeType('text/html');
   } 
   
   else if (window.ActiveXObject) { // IE
      try 
      {
         http_request = new ActiveXObject("Msxml2.XMLHTTP");
      } 
      catch (e) 
      {
         try { http_request = new ActiveXObject("Microsoft.XMLHTTP"); } 
         catch (e) {}
      }
   }
   
   if (!http_request) 
   {
      alert('Cannot create XMLHTTP instance');
      return false;
   }

   http_request.open('POST', url, true);
   http_request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
   http_request.setRequestHeader("Content-length", parameters.length);
   http_request.setRequestHeader("Connection", "close");
   http_request.send(parameters);
}      

function render() 
{
   if (!http_request) return;
   try 
   {
      // only if http_request shows "complete"
      if (http_request.readyState == 4) 
      {
         // only if "OK"
         if (http_request.status == 200) 
         {
            var root = http_request.responseXML.documentElement;
            renderJobs(root.getElementsByTagName('workqueue').item(0));
            renderWorkers(root.getElementsByTagName('consumers').item(0));
            renderCompleted(root.getElementsByTagName('completed').item(0));
         } 
         else 
         {
            if(!error) 
            {
               error = true;
               document.getElementById("errmsg0").innerHTML = "The server is no longer available.<br/>The page will no longer be updated.";
               document.getElementById("errmsg0").style.display = '';
            }
         }
      }
   }
   catch(e) 
   {
      if(!error) 
      {
         error = true;
         document.getElementById("errmsg0").innerHTML     = "The server is no longer available.<br/>The page will no longer be updated.";
         document.getElementById("errmsg0").style.display = '';
      }
   }
}

function renderJobs(wq)
{
   var maxshown = 10;
   var tbl  = document.getElementById("jobs");
   var row  = tbl.rows[0];
   var html = "";
   try 
   {
      for (i=0; i<maxshown; i++)
      {
         if (i >= wq.childNodes.length)
            html += '&nbsp;';
         else
         {
            var job  = wq.childNodes.item(i);
            var id   = job.getElementsByTagName('id')[0].firstChild.data;
            var type = job.getElementsByTagName('type')[0].firstChild.data;
            var bg   = (id % 2) ? 'gainsboro' : 'white';
            html += '<span style="background:' + bg + '">&nbsp;' + id + '&nbsp;</span>';
         }
      }
      row.cells[0].innerHTML = html;
      if (wq.childNodes.length == 0)
         errmsg = "Click the <b>Start</b> button to populate the queue";
      else
      {
    	 var jobCount = wq.childNodes.length;
         errmsg = "There " + (1 == jobCount ? "is " : "are ") + (jobCount) + (1 == jobCount ? " job" : " jobs") + " in the queue";
         var notShownCount = wq.childNodes.length - maxshown;
         if (notShownCount > 0) 
            errmsg += " (" + (notShownCount) + (1 == notShownCount ? " is" : " are") + " not shown)";
      }
      document.getElementById("errmsg1").innerHTML = errmsg;
   }
   catch(e) 
   { document.getElementById("errmsg1").innerHTML = e; }
}

function renderWorkers(wq)
{
   try
   {
      var element = document.getElementById("workers");
      var html    = '<table cellpadding="4" cellspacing="2" style="font-size:18pt">'
      for (i=0; i<wq.childNodes.length; i++)
      {
         var worker = wq.childNodes.item(i);
         var name   = worker.getElementsByTagName('name')[0].firstChild.data;
         var jobs   = worker.getElementsByTagName('jobs').item(0);
         html      += '<tr><td><img src="images/user.gif" alt="' + name + '"/>&nbsp;</td>';
         if (jobs.childNodes.length == 0)
            html += '<td><span style="font-size:8pt">(no jobs available)</span></td>'
         else
         {
            for(j=0; j<jobs.childNodes.length; j++)
            {
               var job  = jobs.childNodes.item(j);
               var id   = job.getElementsByTagName('id')[0].firstChild.data;
               var type = job.getElementsByTagName('type')[0].firstChild.data;
               var bg   = (id % 2) ? 'gainsboro' : 'white';
               html    += '<td style="border:2px solid gray; background:' + bg + '">' + id + '</td>';
            }
            html += '</tr>'
         }
      }
      html += '</table>'
      element.innerHTML = html;
   }
   catch(e) { document.getElementById("errmsg2").innerHTML = e; }
}

function renderCompleted(wq)
{
   try 
   {
      var section = document.getElementById("completed_jobs_section");
      if (wq.childNodes.length > 0)
         section.style.display = '';
         
      var tbl = document.getElementById("completed");
      for (i=0; i<wq.childNodes.length; i++)
      {
         var job      = wq.childNodes.item(i);
         var id       = job.getElementsByTagName('id')[0].firstChild.data;
         var type     = job.getElementsByTagName('type')[0].firstChild.data;
         var producer = job.getElementsByTagName('producer')[0].firstChild.data;
         var consumer = job.getElementsByTagName('consumer')[0].firstChild.data;
         var state    = job.getElementsByTagName('state')[0].firstChild.data;
         var duration = job.getElementsByTagName('duration')[0].firstChild.data;
         var row;
         if (i + 1 >= tbl.rows.length)
         {
            row = tbl.insertRow(tbl.rows.length);
            for (j=0; j<4; j++)
               row.insertCell(j);
         }
         else
            row = tbl.rows[i + 1]
         //row.cells[0].innerHTML = '<img src="images/jobtype' + type +'.gif" style="border:0px solid silver; padding:0px; margin:0px"/>';
         row.cells[0].innerHTML = id;
         row.cells[1].innerHTML = producer;
         row.cells[2].innerHTML = consumer;
         row.cells[3].innerHTML = (duration * 1000) + " ms";
      }
   }
   catch(e) 
   { document.getElementById("errmsg3").innerHTML = e; }
}
