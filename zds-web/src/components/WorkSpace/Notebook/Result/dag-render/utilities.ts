export {
    getSrcDom
  }
  const DAG_PLAN =  '      <div id="plan-viz-metadata" style="display:none">\n' + 
  '<div class="dot-file">\n' + 
  '          digraph G {\n' + 
  '          0 [label=&quot;CollectLimit&quot;];\n' + 
  
  '          subgraph cluster1 {\n' + 
  '          label=&quot;WholeStageCodegen&quot;;\n' + 
  '          2 [label=&quot;HashAggregate&quot;];\n' + 
  '          }\n' + 
  
  '          3 [label=&quot;Exchange&quot;];\n' + 
  
  '          subgraph cluster4 {\n' + 
  '          label=&quot;WholeStageCodegen&quot;;\n' + 
  '          5 [label=&quot;HashAggregate&quot;];\n' + 
  '          6 [label=&quot;HashAggregate&quot;];\n' + 
  '          7 [label=&quot;HashAggregate&quot;];\n' + 
  '          8 [label=&quot;Project&quot;];\n' + 
  '          9 [label=&quot;SortMergeJoin&quot;];\n' + 
  '          }\n' + 
  '          subgraph cluster10 {\n' + 
  '          label=&quot;WholeStageCodegen&quot;;\n' + 
  '          11 [label=&quot;Sort&quot;];\n' + 
  '          }\n' + 
  '          12 [label=&quot;Exchange&quot;];\n' + 
  '          subgraph cluster13 {\n' + 
  '          label=&quot;WholeStageCodegen&quot;;\n' + 
  '          14 [label=&quot;Project&quot;];\n' + 
  '          15 [label=&quot;Filter&quot;];\n' + 
  '          16 [label=&quot;SerializeFromObject&quot;];\n' + 
  '          }\n' + 
  '          17 [label=&quot;ExternalRDDScan&quot;];\n' + 
  '          subgraph cluster18 {\n' + 
  '          label=&quot;WholeStageCodegen&quot;;\n' + 
  '          19 [label=&quot;Sort&quot;];\n' + 
  '          }\n' + 
  '          20 [label=&quot;Exchange\n \ndata size total (min, med, max): \n62.0 B (31.0 B, 31.0 B, 31.0 B)&quot;];\n' + 
  '          subgraph cluster21 {\n' + 
  '          label=&quot;WholeStageCodegen\n\n\n93 ms (46 ms, 47 ms, 47 ms)&quot;;\n' + 
  '          22 [label=&quot;Project&quot;];\n' + 
  '          23 [label=&quot;Filter\n \nnumber of output rows: 4&quot;];\n' + 
  '          24 [label=&quot;SerializeFromObject&quot;];\n' + 
  '          }\n' + 
  '\n' + 
  '          25 [label=&quot;ExternalRDDScan\n \nnumber of output rows: 4&quot;];\n' + 
  '          2-&gt;0;\n' + 
  '\n' + 
  '          3-&gt;2;\n' + 
  '\n' + 
  '          5-&gt;3;\n' + 
  '\n' + 
  '          6-&gt;5;\n' + 
  '\n' + 
  '          7-&gt;6;\n' + 
  '\n' + 
  '          8-&gt;7;\n' + 
  '\n' + 
  '          9-&gt;8;\n' + 
  '\n' + 
  '          11-&gt;9;\n' + 
  '\n' + 
  '          12-&gt;11;\n' + 
  '\n' + 
  '          14-&gt;12;\n' + 
  '\n' + 
  '          15-&gt;14;\n' + 
  '\n' + 
  '          16-&gt;15;\n' + 
  '\n' + 
  '          17-&gt;16;\n' + 
  '\n' + 
  '          19-&gt;9;\n' + 
  '\n' + 
  '          20-&gt;19;\n' + 
  '\n' + 
  '          22-&gt;20;\n' + 
  '\n' + 
  '          23-&gt;22;\n' + 
  '\n' + 
  '          24-&gt;23;\n' + 
  '\n' + 
  '          25-&gt;24;\n' + 
  '\n' + 
  '          }\n' + 
  '        </div>\n' + 
  '        <div id="plan-viz-metadata-size">26</div>\n' + 
  '        <div id="plan-meta-data-0">CollectLimit 21</div>\n' + 
  '        <div id="plan-meta-data-2">HashAggregate(keys=[], functions=[count(1)])</div>\n' + 
  '        <div id="plan-meta-data-1">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-3">Exchange SinglePartition</div>\n' + 
  '        <div id="plan-meta-data-5">HashAggregate(keys=[], functions=[partial_count(1)])</div>\n' + 
  '        <div id="plan-meta-data-6">HashAggregate(keys=[key#6], functions=[])</div>\n' + 
  '        <div id="plan-meta-data-7">HashAggregate(keys=[key#6], functions=[])</div>\n' + 
  '        <div id="plan-meta-data-8">Project [key#6]</div>\n' + 
  '        <div id="plan-meta-data-9">SortMergeJoin [key#6], [key#18], Inner</div>\n' + 
  '        <div id="plan-meta-data-4">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-11">Sort [key#6 ASC NULLS FIRST], false, 0</div>\n' + 
  '        <div id="plan-meta-data-10">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-12">Exchange hashpartitioning(key#6, 5)</div>\n' + 
  '        <div id="plan-meta-data-14">Project [_1#3 AS key#6]</div>\n' + 
  '        <div id="plan-meta-data-15">Filter isnotnull(_1#3)</div>\n' + 
  '        <div id="plan-meta-data-16">SerializeFromObject [assertnotnull(input[0, scala.Tuple2, true])._1 AS _1#3,\n' + 
  '          assertnotnull(input[0, scala.Tuple2, true])._2 AS _2#4]</div>\n' + 
  '        <div id="plan-meta-data-13">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-17">Scan ExternalRDDScan[obj#2]</div>\n' + 
  '        <div id="plan-meta-data-19">Sort [key#18 ASC NULLS FIRST], false, 0</div>\n' + 
  '        <div id="plan-meta-data-18">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-20">Exchange hashpartitioning(key#18, 5)</div>\n' + 
  '        <div id="plan-meta-data-22">Project [_1#15 AS key#18]</div>\n' + 
  '        <div id="plan-meta-data-23">Filter (isnotnull(_2#16) &amp;&amp; isnotnull(_1#15))</div>\n' + 
  '        <div id="plan-meta-data-24">SerializeFromObject [assertnotnull(input[0, scala.Tuple2, true])._1 AS _1#15,\n' + 
  '          assertnotnull(input[0, scala.Tuple2, true])._2 AS _2#16]</div>\n' + 
  '        <div id="plan-meta-data-21">WholeStageCodegen</div>\n' + 
  '        <div id="plan-meta-data-25">Scan ExternalRDDScan[obj#14]</div>' + 
  '      </div>\n';
  const TEMPLATE = '<html> \n' + 
    '<head>\n' + 
    '  <meta http-equiv="Content-type" content="text/html; charset=utf-8" />\n' + 
    '  <link rel="stylesheet" href="${base.res.url}/bootstrap.min.css" type="text/css" />\n' + 
    '  <script src="${base.res.url}/jquery-1.11.1.min.js"></script>\n' + 
    '  <script src="${base.res.url}/bootstrap-tooltip.js"></script>\n' + 
    '  <script src="${base.res.url}/initialize-tooltips.js"></script>\n' + 
    '  <title>DAG</title>\n' + 
    '</head>\n' + 
    '<body>\n' + 
    '  <div class="container-fluid">\n' + 
    '    <div>\n' + 
    '      <div id="plan-viz-graph"></div>\n' + 
    
    '       ${dag.plan}\n' + 
    '      <link rel="stylesheet" href="${base.res.url}/spark-sql-viz.css" type="text/css" />\n' + 
    '      <script src="${base.res.url}/d3.min.js"></script>\n' + 
    '      <script src="${base.res.url}/dagre-d3.min.js"></script>\n' + 
    '      <script src="${base.res.url}/graphlib-dot.min.js"></script>\n' + 
    '      <script src="${base.res.url}/spark-sql-viz.js"></script>\n' + 
    '      <script>$(function () { renderPlanViz(); })</script>\n' + 
    '    </div>\n' + 
    '  </div>\n' + 
    '</body>\n' + 
    '</html>';
  function getSrcDom(dagPlan = DAG_PLAN){
      let template = TEMPLATE.replace(/\$\{base.res.url\}/g,'./assets')
      return template = template.replace(/\$\{dag.plan\}/g,dagPlan)
  }