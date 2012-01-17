package reid_v01;

import java.util.HashMap;
import java.util.Map;

public class AttributedLink {
  
  public final AttributedNode src;
  public final AttributedNode dst; 
  public final Map<String,Object> attrs;   
  
  public AttributedLink(AttributedNode src, AttributedNode dst, Map<String,Object> attrs) {
    this.src = src;
    this.dst = dst;
    this.attrs = new HashMap<String,Object>(attrs);
  }
  
  public Object getAttr(String name) {
    return attrs.get(name);
  }
  
  public void setAttr(String name, Object value) {
    attrs.put(name, value);
  }
  
}
