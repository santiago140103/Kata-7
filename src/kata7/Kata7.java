package kata7;

import com.google.gson.Gson;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import static spark.Spark.*;
import java.util.HashMap;
import java.util.Map;

public class Kata7 {
    
    private static int total;

    public static void main(String[] args) throws SQLException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        //Luego de descomprimir la base de datos y ejecutar el programa pegar el siguiente link en un navegador
        //http://localhost:4567/histogram/DayOfWeek?filters=DayOfWeek=MONDAY,WEDNESDAY;Cancelled=1
        port(4567);
        
        get("/hola/:dim", (request, response) -> {
           String dim = request.params("dim");
           String filters = request.queryParams("filters");
           return filters.equals("");
        });
        
        
        
        get("/histogram/:dimension", (request, response) -> {
            // Obtenga los parámetros de la solicitud
            Map<String,String> dayOfWeekMap = getDayOfWeekMap();
            Map<String,String> fieldsMap = getFieldsMap();
            
            String dimension = request.params("dimension");
            String filters = request.queryParams("filters");
            System.out.println(filters);
            filters = treatFilters(filters, fieldsMap, dayOfWeekMap);
            System.out.println(filters);
            
            FlightStore store = new SqliteFlightStore(new File("flights.db"), filters);
            List<Flight> flights = new ArrayList<>();
            for (Flight flight : store.flights()) 
                flights.add(flight);
            
            //String binSize = request.queryParams("binSize");
            
            // Cree un objeto Histograma con los datos obtenidos
            Histogram histogram = getHistogram(flights, dimension);
            total = 0;
            for (Object key : histogram.keySet()) {
                total += histogram.get(key);
            }
            System.out.println(total);
            
            // Serialice el objeto Histograma a formato JSON usando GSON
            Gson gson = new Gson();
            String json = gson.toJson(histogram);
 
            // Devuelva el JSON como respuesta al cliente
            response.type("application/json");
            return json;
               
        });
        
        
        
    }
    
    public static Histogram getHistogram (List<Flight> flights, String dim) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        dim = dim.substring(0, 1).toLowerCase() + dim.substring(1);
        Field field = flights.get(0).getClass().getDeclaredField(dim);
        field.setAccessible(true);
        
        Histogram histogram = new Histogram();
        
        for (Flight flight : flights) 
            histogram.increment(field.get(flight).toString());
        
        return histogram;
        
    }

   

    private static String treatFilters(String str, Map fieldsMap, Map dayOfWeekMap) {
        String result = "";
        String[] clauses = str.split(";");
        for (int i = 0; i < clauses.length; i++) {
            String[] parts = clauses[i].split("=");
            String field = parts[0];
            String[] values = parts[1].split(",");
            for (int j = 0; j < values.length; j++) {
                
                //result += fieldsMap.get(field) + " = " + dayOfWeekMap.get(values[j]);
                result += fieldsMap.get(field) + " = ";
                result += (field.equals("DayOfWeek")) ? dayOfWeekMap.get(values[j]) : values[j];
                if (j < values.length - 1) {
                    result += " OR ";
                }
            }
            if (i < clauses.length - 1) {
                result += " AND ";
            }
        }
        return result;
    }

    private static Map<String, String> getDayOfWeekMap() {
        Map<String,String> map = new HashMap<>();
        map.put("MONDAY", "1");
        map.put("TUESDAY", "2");
        map.put("WEDNESDAY", "3");
        map.put("THURSDAY", "4");
        map.put("FRIDAY", "5");
        map.put("SATURDAY", "6");
        map.put("SUNDAY", "7");
        
        return map;
    }

    private static Map<String, String> getFieldsMap() {
        Map<String,String> map = new HashMap<>();
        map.put("DayOfWeek", "DAY_OF_WEEK");
        map.put("ArrivalTime", "ARR_TIME");
        map.put("DepartureTime", "DEP_TIME");
        map.put("Distance", "DISTANCE");
        map.put("Cancelled", "CANCELLED");
        map.put("Diverted", "DIVERTED");
        map.put("ArrivalDelay", "ARR_DELAY");
        map.put("DepartureDelay", "DEP_DELAY");
        return map;
    }
}
