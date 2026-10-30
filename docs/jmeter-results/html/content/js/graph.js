/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
$(document).ready(function() {

    $(".click-title").mouseenter( function(    e){
        e.preventDefault();
        this.style.cursor="pointer";
    });
    $(".click-title").mousedown( function(event){
        event.preventDefault();
    });

    // Ugly code while this script is shared among several pages
    try{
        refreshHitsPerSecond(true);
    } catch(e){}
    try{
        refreshResponseTimeOverTime(true);
    } catch(e){}
    try{
        refreshResponseTimePercentiles();
    } catch(e){}
});


var responseTimePercentilesInfos = {
        data: {"result": {"minY": 468.0, "minX": 0.0, "maxY": 1443.0, "series": [{"data": [[0.0, 468.0], [0.1, 468.0], [0.2, 468.0], [0.3, 468.0], [0.4, 469.0], [0.5, 469.0], [0.6, 469.0], [0.7, 470.0], [0.8, 470.0], [0.9, 470.0], [1.0, 470.0], [1.1, 470.0], [1.2, 470.0], [1.3, 470.0], [1.4, 470.0], [1.5, 471.0], [1.6, 471.0], [1.7, 471.0], [1.8, 471.0], [1.9, 471.0], [2.0, 471.0], [2.1, 471.0], [2.2, 472.0], [2.3, 472.0], [2.4, 472.0], [2.5, 473.0], [2.6, 473.0], [2.7, 473.0], [2.8, 473.0], [2.9, 474.0], [3.0, 474.0], [3.1, 474.0], [3.2, 474.0], [3.3, 474.0], [3.4, 474.0], [3.5, 474.0], [3.6, 474.0], [3.7, 474.0], [3.8, 474.0], [3.9, 475.0], [4.0, 475.0], [4.1, 475.0], [4.2, 489.0], [4.3, 489.0], [4.4, 496.0], [4.5, 496.0], [4.6, 496.0], [4.7, 496.0], [4.8, 496.0], [4.9, 507.0], [5.0, 507.0], [5.1, 507.0], [5.2, 507.0], [5.3, 508.0], [5.4, 508.0], [5.5, 508.0], [5.6, 508.0], [5.7, 508.0], [5.8, 508.0], [5.9, 517.0], [6.0, 517.0], [6.1, 517.0], [6.2, 517.0], [6.3, 517.0], [6.4, 521.0], [6.5, 521.0], [6.6, 521.0], [6.7, 522.0], [6.8, 522.0], [6.9, 525.0], [7.0, 525.0], [7.1, 528.0], [7.2, 528.0], [7.3, 531.0], [7.4, 531.0], [7.5, 567.0], [7.6, 567.0], [7.7, 590.0], [7.8, 590.0], [7.9, 590.0], [8.0, 590.0], [8.1, 590.0], [8.2, 590.0], [8.3, 591.0], [8.4, 591.0], [8.5, 591.0], [8.6, 591.0], [8.7, 593.0], [8.8, 593.0], [8.9, 593.0], [9.0, 593.0], [9.1, 594.0], [9.2, 594.0], [9.3, 596.0], [9.4, 599.0], [9.5, 599.0], [9.6, 604.0], [9.7, 604.0], [9.8, 627.0], [9.9, 627.0], [10.0, 634.0], [10.1, 634.0], [10.2, 650.0], [10.3, 650.0], [10.4, 650.0], [10.5, 650.0], [10.6, 651.0], [10.7, 651.0], [10.8, 672.0], [10.9, 672.0], [11.0, 672.0], [11.1, 672.0], [11.2, 674.0], [11.3, 674.0], [11.4, 684.0], [11.5, 684.0], [11.6, 688.0], [11.7, 688.0], [11.8, 688.0], [11.9, 688.0], [12.0, 689.0], [12.1, 689.0], [12.2, 694.0], [12.3, 694.0], [12.4, 705.0], [12.5, 705.0], [12.6, 711.0], [12.7, 711.0], [12.8, 716.0], [12.9, 716.0], [13.0, 720.0], [13.1, 720.0], [13.2, 723.0], [13.3, 723.0], [13.4, 725.0], [13.5, 725.0], [13.6, 734.0], [13.7, 734.0], [13.8, 739.0], [13.9, 739.0], [14.0, 744.0], [14.1, 744.0], [14.2, 745.0], [14.3, 745.0], [14.4, 753.0], [14.5, 753.0], [14.6, 761.0], [14.7, 761.0], [14.8, 762.0], [14.9, 762.0], [15.0, 763.0], [15.1, 763.0], [15.2, 766.0], [15.3, 766.0], [15.4, 766.0], [15.5, 766.0], [15.6, 768.0], [15.7, 768.0], [15.8, 768.0], [15.9, 768.0], [16.0, 768.0], [16.1, 768.0], [16.2, 769.0], [16.3, 769.0], [16.4, 769.0], [16.5, 769.0], [16.6, 770.0], [16.7, 770.0], [16.8, 770.0], [16.9, 770.0], [17.0, 771.0], [17.1, 771.0], [17.2, 772.0], [17.3, 772.0], [17.4, 772.0], [17.5, 772.0], [17.6, 772.0], [17.7, 772.0], [17.8, 772.0], [17.9, 772.0], [18.0, 775.0], [18.1, 775.0], [18.2, 779.0], [18.3, 779.0], [18.4, 781.0], [18.5, 781.0], [18.6, 781.0], [18.7, 781.0], [18.8, 781.0], [18.9, 781.0], [19.0, 784.0], [19.1, 784.0], [19.2, 796.0], [19.3, 796.0], [19.4, 798.0], [19.5, 798.0], [19.6, 801.0], [19.7, 801.0], [19.8, 805.0], [19.9, 805.0], [20.0, 816.0], [20.1, 816.0], [20.2, 816.0], [20.3, 816.0], [20.4, 817.0], [20.5, 817.0], [20.6, 822.0], [20.7, 822.0], [20.8, 822.0], [20.9, 822.0], [21.0, 822.0], [21.1, 822.0], [21.2, 823.0], [21.3, 823.0], [21.4, 823.0], [21.5, 823.0], [21.6, 825.0], [21.7, 825.0], [21.8, 827.0], [21.9, 827.0], [22.0, 828.0], [22.1, 828.0], [22.2, 831.0], [22.3, 831.0], [22.4, 834.0], [22.5, 834.0], [22.6, 837.0], [22.7, 837.0], [22.8, 837.0], [22.9, 837.0], [23.0, 839.0], [23.1, 839.0], [23.2, 844.0], [23.3, 844.0], [23.4, 851.0], [23.5, 851.0], [23.6, 874.0], [23.7, 874.0], [23.8, 875.0], [23.9, 875.0], [24.0, 877.0], [24.1, 877.0], [24.2, 877.0], [24.3, 877.0], [24.4, 878.0], [24.5, 878.0], [24.6, 883.0], [24.7, 883.0], [24.8, 890.0], [24.9, 890.0], [25.0, 892.0], [25.1, 892.0], [25.2, 893.0], [25.3, 893.0], [25.4, 895.0], [25.5, 895.0], [25.6, 899.0], [25.7, 899.0], [25.8, 899.0], [25.9, 899.0], [26.0, 902.0], [26.1, 902.0], [26.2, 905.0], [26.3, 905.0], [26.4, 908.0], [26.5, 908.0], [26.6, 915.0], [26.7, 915.0], [26.8, 916.0], [26.9, 916.0], [27.0, 918.0], [27.1, 918.0], [27.2, 919.0], [27.3, 919.0], [27.4, 928.0], [27.5, 928.0], [27.6, 930.0], [27.7, 930.0], [27.8, 935.0], [27.9, 935.0], [28.0, 937.0], [28.1, 937.0], [28.2, 938.0], [28.3, 938.0], [28.4, 939.0], [28.5, 939.0], [28.6, 940.0], [28.7, 940.0], [28.8, 960.0], [28.9, 960.0], [29.0, 964.0], [29.1, 964.0], [29.2, 968.0], [29.3, 968.0], [29.4, 976.0], [29.5, 976.0], [29.6, 977.0], [29.7, 977.0], [29.8, 985.0], [29.9, 985.0], [30.0, 996.0], [30.1, 996.0], [30.2, 997.0], [30.3, 997.0], [30.4, 1015.0], [30.5, 1015.0], [30.6, 1027.0], [30.7, 1027.0], [30.8, 1035.0], [30.9, 1035.0], [31.0, 1038.0], [31.1, 1038.0], [31.2, 1039.0], [31.3, 1039.0], [31.4, 1041.0], [31.5, 1041.0], [31.6, 1041.0], [31.7, 1041.0], [31.8, 1043.0], [31.9, 1043.0], [32.0, 1043.0], [32.1, 1043.0], [32.2, 1043.0], [32.3, 1043.0], [32.4, 1043.0], [32.5, 1043.0], [32.6, 1046.0], [32.7, 1046.0], [32.8, 1054.0], [32.9, 1054.0], [33.0, 1055.0], [33.1, 1055.0], [33.2, 1058.0], [33.3, 1058.0], [33.4, 1059.0], [33.5, 1059.0], [33.6, 1060.0], [33.7, 1060.0], [33.8, 1072.0], [33.9, 1072.0], [34.0, 1081.0], [34.1, 1081.0], [34.2, 1084.0], [34.3, 1084.0], [34.4, 1086.0], [34.5, 1086.0], [34.6, 1092.0], [34.7, 1092.0], [34.8, 1093.0], [34.9, 1093.0], [35.0, 1102.0], [35.1, 1102.0], [35.2, 1102.0], [35.3, 1102.0], [35.4, 1107.0], [35.5, 1107.0], [35.6, 1117.0], [35.7, 1117.0], [35.8, 1118.0], [35.9, 1118.0], [36.0, 1120.0], [36.1, 1120.0], [36.2, 1120.0], [36.3, 1120.0], [36.4, 1120.0], [36.5, 1120.0], [36.6, 1128.0], [36.7, 1128.0], [36.8, 1129.0], [36.9, 1129.0], [37.0, 1130.0], [37.1, 1130.0], [37.2, 1130.0], [37.3, 1130.0], [37.4, 1131.0], [37.5, 1131.0], [37.6, 1131.0], [37.7, 1131.0], [37.8, 1135.0], [37.9, 1135.0], [38.0, 1141.0], [38.1, 1141.0], [38.2, 1146.0], [38.3, 1146.0], [38.4, 1147.0], [38.5, 1147.0], [38.6, 1147.0], [38.7, 1147.0], [38.8, 1151.0], [38.9, 1151.0], [39.0, 1151.0], [39.1, 1151.0], [39.2, 1153.0], [39.3, 1153.0], [39.4, 1153.0], [39.5, 1153.0], [39.6, 1154.0], [39.7, 1154.0], [39.8, 1155.0], [39.9, 1155.0], [40.0, 1162.0], [40.1, 1162.0], [40.2, 1166.0], [40.3, 1166.0], [40.4, 1170.0], [40.5, 1170.0], [40.6, 1172.0], [40.7, 1172.0], [40.8, 1172.0], [40.9, 1174.0], [41.0, 1174.0], [41.1, 1175.0], [41.2, 1175.0], [41.3, 1177.0], [41.4, 1177.0], [41.5, 1178.0], [41.6, 1178.0], [41.7, 1181.0], [41.8, 1181.0], [41.9, 1182.0], [42.0, 1182.0], [42.1, 1183.0], [42.2, 1183.0], [42.3, 1192.0], [42.4, 1192.0], [42.5, 1195.0], [42.6, 1195.0], [42.7, 1200.0], [42.8, 1200.0], [42.9, 1200.0], [43.0, 1200.0], [43.1, 1201.0], [43.2, 1201.0], [43.3, 1205.0], [43.4, 1205.0], [43.5, 1205.0], [43.6, 1205.0], [43.7, 1207.0], [43.8, 1207.0], [43.9, 1208.0], [44.0, 1208.0], [44.1, 1211.0], [44.2, 1211.0], [44.3, 1212.0], [44.4, 1212.0], [44.5, 1213.0], [44.6, 1213.0], [44.7, 1213.0], [44.8, 1213.0], [44.9, 1213.0], [45.0, 1213.0], [45.1, 1213.0], [45.2, 1213.0], [45.3, 1216.0], [45.4, 1216.0], [45.5, 1218.0], [45.6, 1218.0], [45.7, 1220.0], [45.8, 1220.0], [45.9, 1220.0], [46.0, 1220.0], [46.1, 1221.0], [46.2, 1221.0], [46.3, 1224.0], [46.4, 1224.0], [46.5, 1224.0], [46.6, 1224.0], [46.7, 1226.0], [46.8, 1226.0], [46.9, 1227.0], [47.0, 1227.0], [47.1, 1227.0], [47.2, 1227.0], [47.3, 1228.0], [47.4, 1228.0], [47.5, 1228.0], [47.6, 1228.0], [47.7, 1229.0], [47.8, 1229.0], [47.9, 1230.0], [48.0, 1230.0], [48.1, 1230.0], [48.2, 1230.0], [48.3, 1230.0], [48.4, 1230.0], [48.5, 1231.0], [48.6, 1231.0], [48.7, 1231.0], [48.8, 1231.0], [48.9, 1233.0], [49.0, 1233.0], [49.1, 1234.0], [49.2, 1234.0], [49.3, 1234.0], [49.4, 1234.0], [49.5, 1234.0], [49.6, 1234.0], [49.7, 1239.0], [49.8, 1239.0], [49.9, 1245.0], [50.0, 1245.0], [50.1, 1247.0], [50.2, 1247.0], [50.3, 1250.0], [50.4, 1250.0], [50.5, 1253.0], [50.6, 1253.0], [50.7, 1254.0], [50.8, 1254.0], [50.9, 1255.0], [51.0, 1255.0], [51.1, 1256.0], [51.2, 1256.0], [51.3, 1258.0], [51.4, 1258.0], [51.5, 1260.0], [51.6, 1260.0], [51.7, 1261.0], [51.8, 1261.0], [51.9, 1261.0], [52.0, 1261.0], [52.1, 1261.0], [52.2, 1261.0], [52.3, 1261.0], [52.4, 1261.0], [52.5, 1262.0], [52.6, 1262.0], [52.7, 1263.0], [52.8, 1263.0], [52.9, 1263.0], [53.0, 1263.0], [53.1, 1264.0], [53.2, 1264.0], [53.3, 1264.0], [53.4, 1264.0], [53.5, 1266.0], [53.6, 1266.0], [53.7, 1268.0], [53.8, 1268.0], [53.9, 1269.0], [54.0, 1269.0], [54.1, 1269.0], [54.2, 1269.0], [54.3, 1274.0], [54.4, 1274.0], [54.5, 1274.0], [54.6, 1274.0], [54.7, 1274.0], [54.8, 1274.0], [54.9, 1276.0], [55.0, 1276.0], [55.1, 1276.0], [55.2, 1276.0], [55.3, 1278.0], [55.4, 1278.0], [55.5, 1280.0], [55.6, 1280.0], [55.7, 1281.0], [55.8, 1281.0], [55.9, 1282.0], [56.0, 1282.0], [56.1, 1283.0], [56.2, 1283.0], [56.3, 1283.0], [56.4, 1283.0], [56.5, 1285.0], [56.6, 1285.0], [56.7, 1288.0], [56.8, 1288.0], [56.9, 1290.0], [57.0, 1290.0], [57.1, 1290.0], [57.2, 1290.0], [57.3, 1292.0], [57.4, 1292.0], [57.5, 1294.0], [57.6, 1294.0], [57.7, 1295.0], [57.8, 1295.0], [57.9, 1295.0], [58.0, 1295.0], [58.1, 1297.0], [58.2, 1297.0], [58.3, 1297.0], [58.4, 1297.0], [58.5, 1298.0], [58.6, 1298.0], [58.7, 1299.0], [58.8, 1299.0], [58.9, 1299.0], [59.0, 1299.0], [59.1, 1300.0], [59.2, 1300.0], [59.3, 1301.0], [59.4, 1301.0], [59.5, 1301.0], [59.6, 1301.0], [59.7, 1302.0], [59.8, 1302.0], [59.9, 1302.0], [60.0, 1302.0], [60.1, 1302.0], [60.2, 1302.0], [60.3, 1303.0], [60.4, 1303.0], [60.5, 1303.0], [60.6, 1303.0], [60.7, 1304.0], [60.8, 1304.0], [60.9, 1305.0], [61.0, 1305.0], [61.1, 1305.0], [61.2, 1305.0], [61.3, 1305.0], [61.4, 1305.0], [61.5, 1306.0], [61.6, 1306.0], [61.7, 1307.0], [61.8, 1307.0], [61.9, 1308.0], [62.0, 1308.0], [62.1, 1311.0], [62.2, 1311.0], [62.3, 1311.0], [62.4, 1311.0], [62.5, 1312.0], [62.6, 1312.0], [62.7, 1312.0], [62.8, 1312.0], [62.9, 1313.0], [63.0, 1313.0], [63.1, 1313.0], [63.2, 1313.0], [63.3, 1314.0], [63.4, 1314.0], [63.5, 1316.0], [63.6, 1316.0], [63.7, 1316.0], [63.8, 1316.0], [63.9, 1316.0], [64.0, 1316.0], [64.1, 1317.0], [64.2, 1317.0], [64.3, 1317.0], [64.4, 1317.0], [64.5, 1317.0], [64.6, 1317.0], [64.7, 1318.0], [64.8, 1318.0], [64.9, 1318.0], [65.0, 1318.0], [65.1, 1319.0], [65.2, 1319.0], [65.3, 1319.0], [65.4, 1319.0], [65.5, 1320.0], [65.6, 1320.0], [65.7, 1322.0], [65.8, 1322.0], [65.9, 1323.0], [66.0, 1323.0], [66.1, 1323.0], [66.2, 1323.0], [66.3, 1325.0], [66.4, 1325.0], [66.5, 1326.0], [66.6, 1326.0], [66.7, 1327.0], [66.8, 1327.0], [66.9, 1327.0], [67.0, 1327.0], [67.1, 1327.0], [67.2, 1327.0], [67.3, 1328.0], [67.4, 1328.0], [67.5, 1328.0], [67.6, 1328.0], [67.7, 1328.0], [67.8, 1328.0], [67.9, 1330.0], [68.0, 1330.0], [68.1, 1330.0], [68.2, 1330.0], [68.3, 1330.0], [68.4, 1330.0], [68.5, 1330.0], [68.6, 1330.0], [68.7, 1331.0], [68.8, 1331.0], [68.9, 1332.0], [69.0, 1332.0], [69.1, 1334.0], [69.2, 1334.0], [69.3, 1334.0], [69.4, 1334.0], [69.5, 1334.0], [69.6, 1334.0], [69.7, 1336.0], [69.8, 1336.0], [69.9, 1336.0], [70.0, 1336.0], [70.1, 1337.0], [70.2, 1337.0], [70.3, 1338.0], [70.4, 1338.0], [70.5, 1338.0], [70.6, 1338.0], [70.7, 1338.0], [70.8, 1338.0], [70.9, 1338.0], [71.0, 1338.0], [71.1, 1339.0], [71.2, 1339.0], [71.3, 1339.0], [71.4, 1339.0], [71.5, 1339.0], [71.6, 1339.0], [71.7, 1339.0], [71.8, 1339.0], [71.9, 1340.0], [72.0, 1340.0], [72.1, 1340.0], [72.2, 1340.0], [72.3, 1342.0], [72.4, 1342.0], [72.5, 1342.0], [72.6, 1342.0], [72.7, 1342.0], [72.8, 1342.0], [72.9, 1343.0], [73.0, 1343.0], [73.1, 1344.0], [73.2, 1344.0], [73.3, 1344.0], [73.4, 1344.0], [73.5, 1345.0], [73.6, 1345.0], [73.7, 1346.0], [73.8, 1346.0], [73.9, 1346.0], [74.0, 1346.0], [74.1, 1347.0], [74.2, 1347.0], [74.3, 1347.0], [74.4, 1347.0], [74.5, 1347.0], [74.6, 1347.0], [74.7, 1349.0], [74.8, 1349.0], [74.9, 1349.0], [75.0, 1349.0], [75.1, 1350.0], [75.2, 1350.0], [75.3, 1351.0], [75.4, 1351.0], [75.5, 1351.0], [75.6, 1351.0], [75.7, 1352.0], [75.8, 1352.0], [75.9, 1353.0], [76.0, 1353.0], [76.1, 1353.0], [76.2, 1353.0], [76.3, 1354.0], [76.4, 1354.0], [76.5, 1354.0], [76.6, 1354.0], [76.7, 1354.0], [76.8, 1354.0], [76.9, 1356.0], [77.0, 1356.0], [77.1, 1356.0], [77.2, 1356.0], [77.3, 1357.0], [77.4, 1357.0], [77.5, 1358.0], [77.6, 1358.0], [77.7, 1358.0], [77.8, 1358.0], [77.9, 1358.0], [78.0, 1358.0], [78.1, 1359.0], [78.2, 1359.0], [78.3, 1360.0], [78.4, 1360.0], [78.5, 1360.0], [78.6, 1360.0], [78.7, 1361.0], [78.8, 1361.0], [78.9, 1361.0], [79.0, 1361.0], [79.1, 1361.0], [79.2, 1361.0], [79.3, 1362.0], [79.4, 1362.0], [79.5, 1362.0], [79.6, 1362.0], [79.7, 1362.0], [79.8, 1362.0], [79.9, 1363.0], [80.0, 1363.0], [80.1, 1363.0], [80.2, 1363.0], [80.3, 1364.0], [80.4, 1364.0], [80.5, 1365.0], [80.6, 1365.0], [80.7, 1366.0], [80.8, 1366.0], [80.9, 1367.0], [81.0, 1367.0], [81.1, 1368.0], [81.2, 1368.0], [81.3, 1368.0], [81.4, 1368.0], [81.5, 1368.0], [81.6, 1368.0], [81.7, 1370.0], [81.8, 1370.0], [81.9, 1371.0], [82.0, 1371.0], [82.1, 1373.0], [82.2, 1373.0], [82.3, 1374.0], [82.4, 1374.0], [82.5, 1374.0], [82.6, 1374.0], [82.7, 1374.0], [82.8, 1374.0], [82.9, 1374.0], [83.0, 1374.0], [83.1, 1375.0], [83.2, 1375.0], [83.3, 1375.0], [83.4, 1375.0], [83.5, 1375.0], [83.6, 1375.0], [83.7, 1375.0], [83.8, 1375.0], [83.9, 1375.0], [84.0, 1375.0], [84.1, 1376.0], [84.2, 1376.0], [84.3, 1376.0], [84.4, 1376.0], [84.5, 1376.0], [84.6, 1376.0], [84.7, 1376.0], [84.8, 1376.0], [84.9, 1377.0], [85.0, 1377.0], [85.1, 1377.0], [85.2, 1377.0], [85.3, 1378.0], [85.4, 1378.0], [85.5, 1378.0], [85.6, 1378.0], [85.7, 1378.0], [85.8, 1378.0], [85.9, 1378.0], [86.0, 1378.0], [86.1, 1379.0], [86.2, 1379.0], [86.3, 1379.0], [86.4, 1379.0], [86.5, 1379.0], [86.6, 1379.0], [86.7, 1379.0], [86.8, 1379.0], [86.9, 1379.0], [87.0, 1379.0], [87.1, 1382.0], [87.2, 1382.0], [87.3, 1383.0], [87.4, 1383.0], [87.5, 1383.0], [87.6, 1383.0], [87.7, 1383.0], [87.8, 1383.0], [87.9, 1383.0], [88.0, 1383.0], [88.1, 1383.0], [88.2, 1383.0], [88.3, 1384.0], [88.4, 1384.0], [88.5, 1385.0], [88.6, 1385.0], [88.7, 1385.0], [88.8, 1385.0], [88.9, 1385.0], [89.0, 1385.0], [89.1, 1387.0], [89.2, 1387.0], [89.3, 1388.0], [89.4, 1388.0], [89.5, 1388.0], [89.6, 1388.0], [89.7, 1389.0], [89.8, 1389.0], [89.9, 1390.0], [90.0, 1390.0], [90.1, 1390.0], [90.2, 1390.0], [90.3, 1391.0], [90.4, 1391.0], [90.5, 1391.0], [90.6, 1391.0], [90.7, 1392.0], [90.8, 1392.0], [90.9, 1392.0], [91.0, 1392.0], [91.1, 1392.0], [91.2, 1392.0], [91.3, 1392.0], [91.4, 1392.0], [91.5, 1393.0], [91.6, 1393.0], [91.7, 1393.0], [91.8, 1393.0], [91.9, 1393.0], [92.0, 1393.0], [92.1, 1393.0], [92.2, 1393.0], [92.3, 1393.0], [92.4, 1393.0], [92.5, 1394.0], [92.6, 1394.0], [92.7, 1394.0], [92.8, 1394.0], [92.9, 1395.0], [93.0, 1395.0], [93.1, 1396.0], [93.2, 1396.0], [93.3, 1397.0], [93.4, 1397.0], [93.5, 1398.0], [93.6, 1398.0], [93.7, 1399.0], [93.8, 1399.0], [93.9, 1399.0], [94.0, 1399.0], [94.1, 1399.0], [94.2, 1399.0], [94.3, 1400.0], [94.4, 1400.0], [94.5, 1401.0], [94.6, 1401.0], [94.7, 1402.0], [94.8, 1402.0], [94.9, 1403.0], [95.0, 1403.0], [95.1, 1404.0], [95.2, 1404.0], [95.3, 1404.0], [95.4, 1404.0], [95.5, 1406.0], [95.6, 1406.0], [95.7, 1406.0], [95.8, 1406.0], [95.9, 1407.0], [96.0, 1407.0], [96.1, 1407.0], [96.2, 1407.0], [96.3, 1410.0], [96.4, 1410.0], [96.5, 1411.0], [96.6, 1411.0], [96.7, 1412.0], [96.8, 1412.0], [96.9, 1414.0], [97.0, 1414.0], [97.1, 1416.0], [97.2, 1416.0], [97.3, 1418.0], [97.4, 1418.0], [97.5, 1419.0], [97.6, 1419.0], [97.7, 1419.0], [97.8, 1419.0], [97.9, 1419.0], [98.0, 1419.0], [98.1, 1422.0], [98.2, 1422.0], [98.3, 1423.0], [98.4, 1423.0], [98.5, 1427.0], [98.6, 1427.0], [98.7, 1427.0], [98.8, 1427.0], [98.9, 1428.0], [99.0, 1428.0], [99.1, 1431.0], [99.2, 1431.0], [99.3, 1432.0], [99.4, 1432.0], [99.5, 1441.0], [99.6, 1441.0], [99.7, 1442.0], [99.8, 1442.0], [99.9, 1443.0], [100.0, 1443.0]], "isOverall": false, "label": "抢购", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 100.0, "title": "Response Time Percentiles"}},
        getOptions: function() {
            return {
                series: {
                    points: { show: false }
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimePercentiles'
                },
                xaxis: {
                    tickDecimals: 1,
                    axisLabel: "Percentiles",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Percentile value in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : %x.2 percentile was %y ms"
                },
                selection: { mode: "xy" },
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimePercentiles"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimesPercentiles"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimesPercentiles"), dataset, prepareOverviewOptions(options));
        }
};

/**
 * @param elementId Id of element where we display message
 */
function setEmptyGraph(elementId) {
    $(function() {
        $(elementId).text("No graph series with filter="+seriesFilter);
    });
}

// Response times percentiles
function refreshResponseTimePercentiles() {
    var infos = responseTimePercentilesInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimePercentiles");
        return;
    }
    if (isGraph($("#flotResponseTimesPercentiles"))){
        infos.createGraph();
    } else {
        var choiceContainer = $("#choicesResponseTimePercentiles");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimesPercentiles", "#overviewResponseTimesPercentiles");
        $('#bodyResponseTimePercentiles .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var responseTimeDistributionInfos = {
        data: {"result": {"minY": 14.0, "minX": 400.0, "maxY": 176.0, "series": [{"data": [[1100.0, 38.0], [600.0, 14.0], [1200.0, 82.0], [1300.0, 176.0], [700.0, 36.0], [1400.0, 29.0], [400.0, 24.0], [800.0, 32.0], [900.0, 22.0], [500.0, 24.0], [1000.0, 23.0]], "isOverall": false, "label": "抢购", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 100, "maxX": 1400.0, "title": "Response Time Distribution"}},
        getOptions: function() {
            var granularity = this.data.result.granularity;
            return {
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimeDistribution'
                },
                xaxis:{
                    axisLabel: "Response times in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of responses",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                bars : {
                    show: true,
                    barWidth: this.data.result.granularity
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: function(label, xval, yval, flotItem){
                        return yval + " responses for " + label + " were between " + xval + " and " + (xval + granularity) + " ms";
                    }
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimeDistribution"), prepareData(data.result.series, $("#choicesResponseTimeDistribution")), options);
        }

};

// Response time distribution
function refreshResponseTimeDistribution() {
    var infos = responseTimeDistributionInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimeDistribution");
        return;
    }
    if (isGraph($("#flotResponseTimeDistribution"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimeDistribution");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        $('#footerResponseTimeDistribution .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var syntheticResponseTimeDistributionInfos = {
        data: {"result": {"minY": 24.0, "minX": 0.0, "ticks": [[0, "Requests having \nresponse time <= 500ms"], [1, "Requests having \nresponse time > 500ms and <= 1,500ms"], [2, "Requests having \nresponse time > 1,500ms"], [3, "Requests in error"]], "maxY": 476.0, "series": [{"data": [[0.0, 24.0]], "color": "#9ACD32", "isOverall": false, "label": "Requests having \nresponse time <= 500ms", "isController": false}, {"data": [[1.0, 476.0]], "color": "yellow", "isOverall": false, "label": "Requests having \nresponse time > 500ms and <= 1,500ms", "isController": false}, {"data": [], "color": "orange", "isOverall": false, "label": "Requests having \nresponse time > 1,500ms", "isController": false}, {"data": [], "color": "#FF6347", "isOverall": false, "label": "Requests in error", "isController": false}], "supportsControllersDiscrimination": false, "maxX": 1.0, "title": "Synthetic Response Times Distribution"}},
        getOptions: function() {
            return {
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendSyntheticResponseTimeDistribution'
                },
                xaxis:{
                    axisLabel: "Response times ranges",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                    tickLength:0,
                    min:-0.5,
                    max:3.5
                },
                yaxis: {
                    axisLabel: "Number of responses",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                bars : {
                    show: true,
                    align: "center",
                    barWidth: 0.25,
                    fill:.75
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: function(label, xval, yval, flotItem){
                        return yval + " " + label;
                    }
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var options = this.getOptions();
            prepareOptions(options, data);
            options.xaxis.ticks = data.result.ticks;
            $.plot($("#flotSyntheticResponseTimeDistribution"), prepareData(data.result.series, $("#choicesSyntheticResponseTimeDistribution")), options);
        }

};

// Response time distribution
function refreshSyntheticResponseTimeDistribution() {
    var infos = syntheticResponseTimeDistributionInfos;
    prepareSeries(infos.data, true);
    if (isGraph($("#flotSyntheticResponseTimeDistribution"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesSyntheticResponseTimeDistribution");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        $('#footerSyntheticResponseTimeDistribution .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var activeThreadsOverTimeInfos = {
        data: {"result": {"minY": 252.82000000000022, "minX": 1.79335362E12, "maxY": 252.82000000000022, "series": [{"data": [[1.79335362E12, 252.82000000000022]], "isOverall": false, "label": "500 并发抢购", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.79335362E12, "title": "Active Threads Over Time"}},
        getOptions: function() {
            return {
                series: {
                    stack: true,
                    lines: {
                        show: true,
                        fill: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of active threads",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 6,
                    show: true,
                    container: '#legendActiveThreadsOverTime'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                selection: {
                    mode: 'xy'
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : At %x there were %y active threads"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesActiveThreadsOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotActiveThreadsOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewActiveThreadsOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Active Threads Over Time
function refreshActiveThreadsOverTime(fixTimestamps) {
    var infos = activeThreadsOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotActiveThreadsOverTime"))) {
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesActiveThreadsOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotActiveThreadsOverTime", "#overviewActiveThreadsOverTime");
        $('#footerActiveThreadsOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var timeVsThreadsInfos = {
        data: {"result": {"minY": 483.6562500000001, "minX": 1.0, "maxY": 1442.0, "series": [{"data": [[6.0, 1419.5], [7.0, 1404.6666666666667], [8.0, 1442.0], [9.0, 1393.0], [10.0, 1419.0], [11.0, 1393.0], [15.0, 1390.0], [16.0, 1409.5], [18.0, 1390.5], [20.0, 1391.5], [31.0, 1383.0], [33.0, 1401.0], [32.0, 1383.0], [35.0, 1402.0], [36.0, 1418.0], [39.0, 1403.4285714285713], [40.0, 1378.0], [47.0, 1403.0], [46.0, 1414.0], [48.0, 1412.0], [55.0, 1384.333333333333], [60.0, 1379.5], [63.0, 1375.0], [62.0, 1400.0], [65.0, 1399.0], [64.0, 1376.0], [70.0, 1390.0], [68.0, 1404.3333333333333], [74.0, 1368.0], [78.0, 1378.6666666666667], [80.0, 1383.5], [87.0, 1374.5], [86.0, 1384.0], [88.0, 1373.3333333333333], [94.0, 1371.1666666666667], [98.0, 1392.0], [97.0, 1358.0], [96.0, 1354.0], [103.0, 1368.5], [101.0, 1369.6666666666667], [100.0, 1354.0], [106.0, 1350.0], [110.0, 1358.5], [114.0, 1356.0], [113.0, 1342.0], [127.0, 1374.0], [126.0, 1384.0], [124.0, 1346.7], [134.0, 1353.3333333333333], [131.0, 1335.0], [129.0, 1345.0], [128.0, 1374.0], [143.0, 1364.0], [141.0, 1327.0], [142.0, 1327.0], [138.0, 1330.0], [137.0, 1355.3333333333333], [150.0, 1328.0], [149.0, 1334.6666666666667], [146.0, 1334.4], [159.0, 1331.2], [154.0, 1341.5], [166.0, 1315.3333333333333], [163.0, 1319.0], [162.0, 1333.3333333333333], [174.0, 1347.0], [173.0, 1327.6], [168.0, 1331.0], [181.0, 1305.0], [180.0, 1308.0], [179.0, 1312.0], [178.0, 1326.5], [177.0, 1329.0], [185.0, 1320.25], [199.0, 1308.6666666666667], [197.0, 1312.3333333333333], [193.0, 1326.0], [192.0, 1303.142857142857], [205.0, 1312.0], [203.0, 1316.0], [201.0, 1317.5], [214.0, 1275.0], [213.0, 1281.0], [211.0, 1327.0], [210.0, 1292.8], [223.0, 1264.0], [221.0, 1271.0], [219.0, 1297.0], [218.0, 1284.0], [216.0, 1292.5], [231.0, 1261.0], [230.0, 1270.75], [226.0, 1281.0], [224.0, 1296.0], [239.0, 1265.5], [237.0, 1272.5], [235.0, 1255.0], [233.0, 1259.5], [247.0, 1258.5], [245.0, 1239.0], [244.0, 1258.0], [242.0, 1274.0], [255.0, 1246.0], [251.0, 1228.0], [250.0, 1230.0], [248.0, 1234.0], [270.0, 1216.0], [269.0, 1250.0], [268.0, 1236.5], [266.0, 1234.6666666666667], [263.0, 1234.25], [259.0, 1221.0], [258.0, 1227.0], [257.0, 1238.0], [286.0, 1182.0], [287.0, 1183.0], [285.0, 1209.0], [275.0, 1220.5], [273.0, 1213.0], [272.0, 1213.0], [283.0, 1228.0], [282.0, 1231.0], [281.0, 1233.0], [280.0, 1195.0], [279.0, 1202.0], [276.0, 1205.0], [302.0, 1146.0], [303.0, 1147.0], [301.0, 1149.0], [299.0, 1167.0], [297.0, 1170.0], [296.0, 1191.5], [294.0, 1191.5], [292.0, 1187.0], [290.0, 1178.0], [289.0, 1213.0], [288.0, 1177.0], [307.0, 1160.0], [318.0, 1115.0], [319.0, 1102.0], [317.0, 1107.0], [315.0, 1151.0], [314.0, 1131.0], [311.0, 1155.0], [310.0, 1141.0], [305.0, 1135.0], [304.0, 1141.0], [334.0, 1055.0], [335.0, 1046.0], [333.0, 1058.0], [332.0, 1060.0], [331.0, 1059.0], [330.0, 1072.0], [329.0, 1082.5], [327.0, 1117.5], [322.0, 1111.5], [321.0, 1120.0], [326.0, 1092.0], [324.0, 1130.0], [349.0, 997.0], [350.0, 976.0], [348.0, 1035.0], [347.0, 1015.0], [346.0, 1054.0], [345.0, 1027.0], [344.0, 1052.0], [340.0, 1041.0], [336.0, 1043.0], [337.0, 1043.0], [365.0, 908.0], [366.0, 905.0], [364.0, 938.0], [354.0, 977.0], [353.0, 980.3333333333334], [363.0, 928.0], [361.0, 941.5], [359.0, 918.0], [358.0, 968.0], [357.0, 935.0], [356.0, 938.0], [381.0, 877.0], [383.0, 861.0], [380.0, 851.0], [369.0, 915.0], [368.0, 900.5], [379.0, 875.0], [378.0, 875.5], [376.0, 899.0], [375.0, 890.0], [374.0, 929.0], [372.0, 893.3333333333334], [399.0, 817.75], [395.0, 829.5], [393.0, 822.0], [392.0, 823.0], [391.0, 825.0], [385.0, 838.0], [390.0, 828.0], [389.0, 829.0], [387.0, 834.0], [386.0, 883.0], [409.0, 775.0], [408.0, 779.0], [407.0, 787.0], [403.0, 797.0], [401.0, 801.0], [400.0, 823.0], [429.0, 739.0], [430.0, 770.0], [428.0, 744.0], [419.0, 769.0], [418.0, 771.5], [427.0, 745.0], [426.0, 772.5], [424.0, 762.0], [423.0, 763.0], [422.0, 767.0], [420.0, 767.3333333333334], [435.0, 734.0], [447.0, 650.3333333333334], [445.0, 688.0], [443.0, 694.0], [442.0, 688.5], [440.0, 672.0], [439.0, 688.5], [437.0, 723.0], [436.0, 725.0], [434.0, 711.0], [433.0, 729.6666666666666], [463.0, 567.0], [462.0, 591.0], [461.0, 591.5714285714286], [454.0, 596.0], [453.0, 599.0], [452.0, 615.5], [450.0, 634.0], [449.0, 679.0], [468.0, 524.0], [465.0, 531.0], [500.0, 483.6562500000001], [1.0, 1399.0]], "isOverall": false, "label": "抢购", "isController": false}, {"data": [[252.82000000000022, 1117.4240000000004]], "isOverall": false, "label": "抢购-Aggregated", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 500.0, "title": "Time VS Threads"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    axisLabel: "Number of active threads",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response times in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: { noColumns: 2,show: true, container: '#legendTimeVsThreads' },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s: At %x.2 active threads, Average response time was %y.2 ms"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesTimeVsThreads"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotTimesVsThreads"), dataset, options);
            // setup overview
            $.plot($("#overviewTimesVsThreads"), dataset, prepareOverviewOptions(options));
        }
};

// Time vs threads
function refreshTimeVsThreads(){
    var infos = timeVsThreadsInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyTimeVsThreads");
        return;
    }
    if(isGraph($("#flotTimesVsThreads"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTimeVsThreads");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTimesVsThreads", "#overviewTimesVsThreads");
        $('#footerTimeVsThreads .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var bytesThroughputOverTimeInfos = {
        data : {"result": {"minY": 4502.933333333333, "minX": 1.79335362E12, "maxY": 4646.55, "series": [{"data": [[1.79335362E12, 4502.933333333333]], "isOverall": false, "label": "Bytes received per second", "isController": false}, {"data": [[1.79335362E12, 4646.55]], "isOverall": false, "label": "Bytes sent per second", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.79335362E12, "title": "Bytes Throughput Over Time"}},
        getOptions : function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity) ,
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Bytes / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendBytesThroughputOverTime'
                },
                selection: {
                    mode: "xy"
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y"
                }
            };
        },
        createGraph : function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesBytesThroughputOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotBytesThroughputOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewBytesThroughputOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Bytes throughput Over Time
function refreshBytesThroughputOverTime(fixTimestamps) {
    var infos = bytesThroughputOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotBytesThroughputOverTime"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesBytesThroughputOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotBytesThroughputOverTime", "#overviewBytesThroughputOverTime");
        $('#footerBytesThroughputOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var responseTimesOverTimeInfos = {
        data: {"result": {"minY": 1117.4240000000004, "minX": 1.79335362E12, "maxY": 1117.4240000000004, "series": [{"data": [[1.79335362E12, 1117.4240000000004]], "isOverall": false, "label": "抢购", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.79335362E12, "title": "Response Time Over Time"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average response time was %y ms"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Response Times Over Time
function refreshResponseTimeOverTime(fixTimestamps) {
    var infos = responseTimesOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimeOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotResponseTimesOverTime"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimesOverTime", "#overviewResponseTimesOverTime");
        $('#footerResponseTimesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var latenciesOverTimeInfos = {
        data: {"result": {"minY": 1112.4020000000025, "minX": 1.79335362E12, "maxY": 1112.4020000000025, "series": [{"data": [[1.79335362E12, 1112.4020000000025]], "isOverall": false, "label": "抢购", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.79335362E12, "title": "Latencies Over Time"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response latencies in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendLatenciesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average latency was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesLatenciesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotLatenciesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewLatenciesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Latencies Over Time
function refreshLatenciesOverTime(fixTimestamps) {
    var infos = latenciesOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyLatenciesOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotLatenciesOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesLatenciesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotLatenciesOverTime", "#overviewLatenciesOverTime");
        $('#footerLatenciesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var connectTimeOverTimeInfos = {
        data: {"result": {"minY": 504.08799999999997, "minX": 1.79335362E12, "maxY": 504.08799999999997, "series": [{"data": [[1.79335362E12, 504.08799999999997]], "isOverall": false, "label": "抢购", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.79335362E12, "title": "Connect Time Over Time"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getConnectTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average Connect Time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendConnectTimeOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average connect time was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesConnectTimeOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotConnectTimeOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewConnectTimeOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Connect Time Over Time
function refreshConnectTimeOverTime(fixTimestamps) {
    var infos = connectTimeOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyConnectTimeOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotConnectTimeOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesConnectTimeOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotConnectTimeOverTime", "#overviewConnectTimeOverTime");
        $('#footerConnectTimeOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var responseTimePercentilesOverTimeInfos = {
        data: {"result": {"minY": 468.0, "minX": 1.79335362E12, "maxY": 1443.0, "series": [{"data": [[1.79335362E12, 1443.0]], "isOverall": false, "label": "Max", "isController": false}, {"data": [[1.79335362E12, 468.0]], "isOverall": false, "label": "Min", "isController": false}, {"data": [[1.79335362E12, 1390.0]], "isOverall": false, "label": "90th percentile", "isController": false}, {"data": [[1.79335362E12, 1430.97]], "isOverall": false, "label": "99th percentile", "isController": false}, {"data": [[1.79335362E12, 1246.0]], "isOverall": false, "label": "Median", "isController": false}, {"data": [[1.79335362E12, 1403.95]], "isOverall": false, "label": "95th percentile", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.79335362E12, "title": "Response Time Percentiles Over Time (successful requests only)"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true,
                        fill: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Response Time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimePercentilesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Response time was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimePercentilesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimePercentilesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimePercentilesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Response Time Percentiles Over Time
function refreshResponseTimePercentilesOverTime(fixTimestamps) {
    var infos = responseTimePercentilesOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotResponseTimePercentilesOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesResponseTimePercentilesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimePercentilesOverTime", "#overviewResponseTimePercentilesOverTime");
        $('#footerResponseTimePercentilesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var responseTimeVsRequestInfos = {
    data: {"result": {"minY": 1225.0, "minX": 34.0, "maxY": 1393.0, "series": [{"data": [[34.0, 1393.0], [466.0, 1225.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 466.0, "title": "Response Time Vs Request"}},
    getOptions: function() {
        return {
            series: {
                lines: {
                    show: false
                },
                points: {
                    show: true
                }
            },
            xaxis: {
                axisLabel: "Global number of requests per second",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            yaxis: {
                axisLabel: "Median Response Time in ms",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            legend: {
                noColumns: 2,
                show: true,
                container: '#legendResponseTimeVsRequest'
            },
            selection: {
                mode: 'xy'
            },
            grid: {
                hoverable: true // IMPORTANT! this is needed for tooltip to work
            },
            tooltip: true,
            tooltipOpts: {
                content: "%s : Median response time at %x req/s was %y ms"
            },
            colors: ["#9ACD32", "#FF6347"]
        };
    },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesResponseTimeVsRequest"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotResponseTimeVsRequest"), dataset, options);
        // setup overview
        $.plot($("#overviewResponseTimeVsRequest"), dataset, prepareOverviewOptions(options));

    }
};

// Response Time vs Request
function refreshResponseTimeVsRequest() {
    var infos = responseTimeVsRequestInfos;
    prepareSeries(infos.data);
    if (isGraph($("#flotResponseTimeVsRequest"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimeVsRequest");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimeVsRequest", "#overviewResponseTimeVsRequest");
        $('#footerResponseRimeVsRequest .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var latenciesVsRequestInfos = {
    data: {"result": {"minY": 1225.0, "minX": 34.0, "maxY": 1393.0, "series": [{"data": [[34.0, 1393.0], [466.0, 1225.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 466.0, "title": "Latencies Vs Request"}},
    getOptions: function() {
        return{
            series: {
                lines: {
                    show: false
                },
                points: {
                    show: true
                }
            },
            xaxis: {
                axisLabel: "Global number of requests per second",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            yaxis: {
                axisLabel: "Median Latency in ms",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            legend: { noColumns: 2,show: true, container: '#legendLatencyVsRequest' },
            selection: {
                mode: 'xy'
            },
            grid: {
                hoverable: true // IMPORTANT! this is needed for tooltip to work
            },
            tooltip: true,
            tooltipOpts: {
                content: "%s : Median Latency time at %x req/s was %y ms"
            },
            colors: ["#9ACD32", "#FF6347"]
        };
    },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesLatencyVsRequest"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotLatenciesVsRequest"), dataset, options);
        // setup overview
        $.plot($("#overviewLatenciesVsRequest"), dataset, prepareOverviewOptions(options));
    }
};

// Latencies vs Request
function refreshLatenciesVsRequest() {
        var infos = latenciesVsRequestInfos;
        prepareSeries(infos.data);
        if(isGraph($("#flotLatenciesVsRequest"))){
            infos.createGraph();
        }else{
            var choiceContainer = $("#choicesLatencyVsRequest");
            createLegend(choiceContainer, infos);
            infos.createGraph();
            setGraphZoomable("#flotLatenciesVsRequest", "#overviewLatenciesVsRequest");
            $('#footerLatenciesVsRequest .legendColorBox > div').each(function(i){
                $(this).clone().prependTo(choiceContainer.find("li").eq(i));
            });
        }
};

var hitsPerSecondInfos = {
        data: {"result": {"minY": 8.333333333333334, "minX": 1.79335362E12, "maxY": 8.333333333333334, "series": [{"data": [[1.79335362E12, 8.333333333333334]], "isOverall": false, "label": "hitsPerSecond", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.79335362E12, "title": "Hits Per Second"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of hits / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendHitsPerSecond"
                },
                selection: {
                    mode : 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y.2 hits/sec"
                }
            };
        },
        createGraph: function createGraph() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesHitsPerSecond"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotHitsPerSecond"), dataset, options);
            // setup overview
            $.plot($("#overviewHitsPerSecond"), dataset, prepareOverviewOptions(options));
        }
};

// Hits per second
function refreshHitsPerSecond(fixTimestamps) {
    var infos = hitsPerSecondInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if (isGraph($("#flotHitsPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesHitsPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotHitsPerSecond", "#overviewHitsPerSecond");
        $('#footerHitsPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var codesPerSecondInfos = {
        data: {"result": {"minY": 8.333333333333334, "minX": 1.79335362E12, "maxY": 8.333333333333334, "series": [{"data": [[1.79335362E12, 8.333333333333334]], "isOverall": false, "label": "200", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.79335362E12, "title": "Codes Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of responses / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendCodesPerSecond"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "Number of Response Codes %s at %x was %y.2 responses / sec"
                }
            };
        },
    createGraph: function() {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesCodesPerSecond"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotCodesPerSecond"), dataset, options);
        // setup overview
        $.plot($("#overviewCodesPerSecond"), dataset, prepareOverviewOptions(options));
    }
};

// Codes per second
function refreshCodesPerSecond(fixTimestamps) {
    var infos = codesPerSecondInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotCodesPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesCodesPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotCodesPerSecond", "#overviewCodesPerSecond");
        $('#footerCodesPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var transactionsPerSecondInfos = {
        data: {"result": {"minY": 8.333333333333334, "minX": 1.79335362E12, "maxY": 8.333333333333334, "series": [{"data": [[1.79335362E12, 8.333333333333334]], "isOverall": false, "label": "抢购-success", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.79335362E12, "title": "Transactions Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of transactions / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendTransactionsPerSecond"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y transactions / sec"
                }
            };
        },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesTransactionsPerSecond"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotTransactionsPerSecond"), dataset, options);
        // setup overview
        $.plot($("#overviewTransactionsPerSecond"), dataset, prepareOverviewOptions(options));
    }
};

// Transactions per second
function refreshTransactionsPerSecond(fixTimestamps) {
    var infos = transactionsPerSecondInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyTransactionsPerSecond");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotTransactionsPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTransactionsPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTransactionsPerSecond", "#overviewTransactionsPerSecond");
        $('#footerTransactionsPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var totalTPSInfos = {
        data: {"result": {"minY": 8.333333333333334, "minX": 1.79335362E12, "maxY": 8.333333333333334, "series": [{"data": [[1.79335362E12, 8.333333333333334]], "isOverall": false, "label": "Transaction-success", "isController": false}, {"data": [], "isOverall": false, "label": "Transaction-failure", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.79335362E12, "title": "Total Transactions Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of transactions / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendTotalTPS"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y transactions / sec"
                },
                colors: ["#9ACD32", "#FF6347"]
            };
        },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesTotalTPS"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotTotalTPS"), dataset, options);
        // setup overview
        $.plot($("#overviewTotalTPS"), dataset, prepareOverviewOptions(options));
    }
};

// Total Transactions per second
function refreshTotalTPS(fixTimestamps) {
    var infos = totalTPSInfos;
    // We want to ignore seriesFilter
    prepareSeries(infos.data, false, true);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, 28800000);
    }
    if(isGraph($("#flotTotalTPS"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTotalTPS");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTotalTPS", "#overviewTotalTPS");
        $('#footerTotalTPS .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

// Collapse the graph matching the specified DOM element depending the collapsed
// status
function collapse(elem, collapsed){
    if(collapsed){
        $(elem).parent().find(".fa-chevron-up").removeClass("fa-chevron-up").addClass("fa-chevron-down");
    } else {
        $(elem).parent().find(".fa-chevron-down").removeClass("fa-chevron-down").addClass("fa-chevron-up");
        if (elem.id == "bodyBytesThroughputOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshBytesThroughputOverTime(true);
            }
            document.location.href="#bytesThroughputOverTime";
        } else if (elem.id == "bodyLatenciesOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshLatenciesOverTime(true);
            }
            document.location.href="#latenciesOverTime";
        } else if (elem.id == "bodyCustomGraph") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshCustomGraph(true);
            }
            document.location.href="#responseCustomGraph";
        } else if (elem.id == "bodyConnectTimeOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshConnectTimeOverTime(true);
            }
            document.location.href="#connectTimeOverTime";
        } else if (elem.id == "bodyResponseTimePercentilesOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimePercentilesOverTime(true);
            }
            document.location.href="#responseTimePercentilesOverTime";
        } else if (elem.id == "bodyResponseTimeDistribution") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimeDistribution();
            }
            document.location.href="#responseTimeDistribution" ;
        } else if (elem.id == "bodySyntheticResponseTimeDistribution") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshSyntheticResponseTimeDistribution();
            }
            document.location.href="#syntheticResponseTimeDistribution" ;
        } else if (elem.id == "bodyActiveThreadsOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshActiveThreadsOverTime(true);
            }
            document.location.href="#activeThreadsOverTime";
        } else if (elem.id == "bodyTimeVsThreads") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTimeVsThreads();
            }
            document.location.href="#timeVsThreads" ;
        } else if (elem.id == "bodyCodesPerSecond") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshCodesPerSecond(true);
            }
            document.location.href="#codesPerSecond";
        } else if (elem.id == "bodyTransactionsPerSecond") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTransactionsPerSecond(true);
            }
            document.location.href="#transactionsPerSecond";
        } else if (elem.id == "bodyTotalTPS") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTotalTPS(true);
            }
            document.location.href="#totalTPS";
        } else if (elem.id == "bodyResponseTimeVsRequest") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimeVsRequest();
            }
            document.location.href="#responseTimeVsRequest";
        } else if (elem.id == "bodyLatenciesVsRequest") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshLatenciesVsRequest();
            }
            document.location.href="#latencyVsRequest";
        }
    }
}

/*
 * Activates or deactivates all series of the specified graph (represented by id parameter)
 * depending on checked argument.
 */
function toggleAll(id, checked){
    var placeholder = document.getElementById(id);

    var cases = $(placeholder).find(':checkbox');
    cases.prop('checked', checked);
    $(cases).parent().children().children().toggleClass("legend-disabled", !checked);

    var choiceContainer;
    if ( id == "choicesBytesThroughputOverTime"){
        choiceContainer = $("#choicesBytesThroughputOverTime");
        refreshBytesThroughputOverTime(false);
    } else if(id == "choicesResponseTimesOverTime"){
        choiceContainer = $("#choicesResponseTimesOverTime");
        refreshResponseTimeOverTime(false);
    }else if(id == "choicesResponseCustomGraph"){
        choiceContainer = $("#choicesResponseCustomGraph");
        refreshCustomGraph(false);
    } else if ( id == "choicesLatenciesOverTime"){
        choiceContainer = $("#choicesLatenciesOverTime");
        refreshLatenciesOverTime(false);
    } else if ( id == "choicesConnectTimeOverTime"){
        choiceContainer = $("#choicesConnectTimeOverTime");
        refreshConnectTimeOverTime(false);
    } else if ( id == "choicesResponseTimePercentilesOverTime"){
        choiceContainer = $("#choicesResponseTimePercentilesOverTime");
        refreshResponseTimePercentilesOverTime(false);
    } else if ( id == "choicesResponseTimePercentiles"){
        choiceContainer = $("#choicesResponseTimePercentiles");
        refreshResponseTimePercentiles();
    } else if(id == "choicesActiveThreadsOverTime"){
        choiceContainer = $("#choicesActiveThreadsOverTime");
        refreshActiveThreadsOverTime(false);
    } else if ( id == "choicesTimeVsThreads"){
        choiceContainer = $("#choicesTimeVsThreads");
        refreshTimeVsThreads();
    } else if ( id == "choicesSyntheticResponseTimeDistribution"){
        choiceContainer = $("#choicesSyntheticResponseTimeDistribution");
        refreshSyntheticResponseTimeDistribution();
    } else if ( id == "choicesResponseTimeDistribution"){
        choiceContainer = $("#choicesResponseTimeDistribution");
        refreshResponseTimeDistribution();
    } else if ( id == "choicesHitsPerSecond"){
        choiceContainer = $("#choicesHitsPerSecond");
        refreshHitsPerSecond(false);
    } else if(id == "choicesCodesPerSecond"){
        choiceContainer = $("#choicesCodesPerSecond");
        refreshCodesPerSecond(false);
    } else if ( id == "choicesTransactionsPerSecond"){
        choiceContainer = $("#choicesTransactionsPerSecond");
        refreshTransactionsPerSecond(false);
    } else if ( id == "choicesTotalTPS"){
        choiceContainer = $("#choicesTotalTPS");
        refreshTotalTPS(false);
    } else if ( id == "choicesResponseTimeVsRequest"){
        choiceContainer = $("#choicesResponseTimeVsRequest");
        refreshResponseTimeVsRequest();
    } else if ( id == "choicesLatencyVsRequest"){
        choiceContainer = $("#choicesLatencyVsRequest");
        refreshLatenciesVsRequest();
    }
    var color = checked ? "black" : "#818181";
    if(choiceContainer != null) {
        choiceContainer.find("label").each(function(){
            this.style.color = color;
        });
    }
}

