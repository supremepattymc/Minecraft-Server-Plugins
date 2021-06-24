import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class Main 
{
   public static void main(String args[]) {
      //Creating a JsonObject object
      JsonArray jsonArray = new JsonArray();
      jsonArray.add("Element A");
      jsonArray.add("Element B");
      jsonArray.add("Element C");
      jsonArray.add("Element D");
      jsonArray.add("Element E");
      
      for(int i = 0; i < jsonArray.size(); ++i)
      {
    	  System.out.println(jsonArray.get(i));
      }
      
      for(JsonElement jsonElement : jsonArray)
      {
    	  System.out.println(jsonElement);
      }
      
      /**	Output
       * 	"Element A"
			"Element B"
			"Element C"
			"Element D"
			"Element E"
			"Element A"
			"Element B"
			"Element C"
			"Element D"
			"Element E"
       */
   }
}
