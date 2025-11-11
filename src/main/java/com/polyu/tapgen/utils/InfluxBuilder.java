package com.polyu.tapgen.utils;


import com.polyu.tapgen.config.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class InfluxBuilder {

    private StringBuffer flux = new StringBuffer();
    private String bucket = Constants.Influx.BUCKET;
    private String measurement = Constants.Influx.MEASUREMENT;
    public InfluxBuilder(){
        init();
    }
    private void init(){
        flux.append("from(bucket: \"").append(bucket).append("\") ");
    }

    public InfluxBuilder time(String startTime, String endTime){
        flux.append("  |> range(start: time(v: \"").append(startTime).append("\"), stop: time(v: \"").append(endTime).append("\"))");
        flux.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")");
        return this;
    }
    public InfluxBuilder time(String startTime, String endTime, String measurement){
        flux.append("  |> range(start: time(v: \"").append(startTime).append("\"), stop: time(v: \"").append(endTime).append("\"))");
        flux.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")");
        return this;
    }
    public InfluxBuilder filter(String key, String value){
        if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)){
            flux.append("  |> filter(fn: (r) => r[\"").append(key).append("\"] == \"").append(value).append("\")");
        }
        return this;
    }

    /**
     * 用于是否有all的情况
     * @param key
     * @param value
     * @return
     */
    public InfluxBuilder filterIotId(String key, String value){
        if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value) && !StringUtils.equalsIgnoreCase(value,"all")){
            flux.append("  |> filter(fn: (r) => r[\"").append(key).append("\"] == \"").append(value).append("\")");
        }
        return this;
    }
    public InfluxBuilder group(String column){
        flux.append("  |> group(columns: [ \"").append(column).append("\"])");
        return this;
    }
    public InfluxBuilder field(String field){
        flux.append("  |> filter(fn: (r) => r._field == \"").append(field).append("\")");
        return this;
    }
    public InfluxBuilder value(String calculationSymbol, Double value){
        flux.append("  |> filter(fn: (r) => r._value ").append(calculationSymbol).append(value).append(")");
        return this;
    }
    public InfluxBuilder spread(Boolean spread){
        if(spread){
            flux.append("  |> spread()");
        }
        return this;
    }
    public InfluxBuilder min(Boolean min){
        if(min){
            flux.append("  |> min()");
        }
        return this;
    }
    public InfluxBuilder filterLike(String field, String query){
        flux.append("  |> filter(fn: (r) => r[\"").append(field).append("\"] =~ /").append(query).append("/) ");
        return this;
    }
    public InfluxBuilder filterLike(String field, String query, String orQuery){
        flux.append("  |> filter(fn: (r) => r[\"").append(field).append("\"] =~ /").append(query).append("/ or r[\"").append(field).append("\"] == \"").append(orQuery).append("\") ");
        return this;
    }

    //contains
    public InfluxBuilder filterContains(String field, List<Object> query){
        if(StringUtils.isNotBlank(field) && !query.isEmpty()) {
            StringBuilder filterSql = new StringBuilder();
            filterSql.append("  |> filter(fn: (r) => (");
            for (int i = 0; i < query.size(); i++) {
                filterSql.append("r[\"").append(field).append("\"] == \"").append(query.get(i)).append("\"");
                if (i < query.size() - 1) {
                    filterSql.append(" or ");
                }
            }
            filterSql.append("))");
            flux.append(filterSql);
        }
        return this;
    }

    /**
     *   |> filter(fn: (r) => (r["code"] == "Test_TEMP" and r["code"] == "status") or (r["code"] == "Test_TEMP" and r["code"] == "status"))
     * @param list
     * @return
     */
    public InfluxBuilder filter(List<Map<String, Object>> list){
        if(list != null && list.size() > 0){
            StringBuilder filterSql = new StringBuilder();
            filterSql.append("  |> filter(fn: (r) => ");
            for(int i = 0; i < list.size(); i++){
                filterSql.append("(");
                Map<String, Object> map = list.get(i);
                int j = 0;
                for(String field: map.keySet()){
                    if(j!=0) filterSql.append(" and ");
                    filterSql.append("r[\"").append(field).append("\"] == \"").append(map.get(field)).append("\"");
                    j ++;
                }
                filterSql.append(")");
                if (i < list.size() - 1) {
                    filterSql.append(" or ");
                }
            }
            filterSql.append(")");
            flux.append(filterSql);
        }

        return this;
    }
    public InfluxBuilder max(Boolean max){
        if(max){
            flux.append("  |> max()");
        }
        return this;
    }
    public InfluxBuilder sum(Boolean sum){
        if(sum){
            flux.append("  |> sum()");
        }
        return this;
    }
    public InfluxBuilder last(Boolean last){
        if(last){
            flux.append("  |> last()");
        }
        return this;
    }
    public InfluxBuilder mean(Boolean mean){
        if(mean){
            flux.append("  |> mean()");
        }
        return this;
    }
    public InfluxBuilder fill(Boolean fill){
        if(fill){
            flux.append("  |> fill(usePrevious: ").append(fill).append(")");
        }
        return this;
    }
    public InfluxBuilder map(String key){
        if(StringUtils.isNotBlank(key)){
            flux.append("|> map(fn: (r) => ({key: ").append(key).append(", value: r._value, time: r._time}))");
        }
        return this;
    }
    public InfluxBuilder aggregate(String every, String fn){
        flux.append("  |> aggregateWindow(every: ").append(every).append(", fn: ").append(fn).append(")");
        return this;
    }
    public InfluxBuilder sort(InfluxSort sort){
        if(sort != null){
            flux.append("  |> sort(columns: [\"").append(sort.getColumn()).append("\"] , desc: ").append(sort.getDesc()).append(")");
        }
        return this;
    }
    public String build(){
        //System.out.println(flux.toString());
        return flux.toString();
    }
}
