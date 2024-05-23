package websocket;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;

import java.util.Base64;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Controll.Contanst.SessionAtrr;
import Controll.Entity.User;
import Controll.Entity.room;
import Controll.Service.MessageService;
import Controll.Service.RoomService;
import Controll.Service.Impl.MessageServiceImp;
import Controll.Service.Impl.RoomServiceImp;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;



@ServerEndpoint(value="/chat/{room}", configurator = HttpSessionConfigurator.class)
public class ChatServlet {

	
	private static final Map<String, Set<Session>> rooms = Collections.synchronizedMap(new HashMap<>());
	private RoomService roomService = new RoomServiceImp();
	private MessageService messageService = new MessageServiceImp();
	
    @OnOpen
    public void handleOpen(Session session, @PathParam("room") String room, EndpointConfig config) {
    	HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (httpSession != null) {
            User user = (User) httpSession.getAttribute(SessionAtrr.CURRENT_USER);
            if (user != null) {
                session.getUserProperties().put("user", user);
            }
        }
        session.getUserProperties().put("room", room);

        rooms.putIfAbsent(room, Collections.synchronizedSet(new HashSet<>()));
        rooms.get(room).add(session);

        System.out.println("Session opened and bound to room: " + room);
    }
	
	@OnMessage
	public void handleMessage(String message, Session userSession) throws IOException {
	    String roomid = (String) userSession.getUserProperties().get("room");
	    User user = (User) userSession.getUserProperties().get("user");
	  
	    
	    
	    
	    try {
	    	
	    	JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
	    	System.out.println(jsonMessage);
	    	String action = jsonMessage.get("action").getAsString();

	    	if ("send".equals(action)) {
	            room room = roomService.findbyid(Integer.parseInt(roomid));
	            String msgContent = jsonMessage.get("messages").getAsString();
	            
	            String fileData = jsonMessage.get("file").getAsString();
	            if(fileData.length() > 0) {
	            	
	            	messageService.createMessage(user, room, msgContent, fileData);
		            
	            }
	            else {
	            	messageService.createMessage(user, room, msgContent, null);
	            }
	            

	           
	            JsonObject response = new JsonObject();
	            response.addProperty("username", user.getFullname());
	            response.addProperty("id", user.getId());
	            response.addProperty("body", msgContent);
	            response.addProperty("action", "send");
	            if (fileData.length() > 0) {
	                response.addProperty("link", fileData);
	            }
	            
	            for (Session session : rooms.get(roomid)) {
	                session.getBasicRemote().sendText(response.toString());
	            }
	        }
	    	else if("delete".equals(action)) {
	    		 int messageId = jsonMessage.get("id").getAsInt();
	             messageService.deleteMessage(messageId);
	             
	             
	             for (Session session : rooms.get(roomid)) {
	                 JsonObject deleteResponse = new JsonObject();
	                 deleteResponse.addProperty("action", "delete");
	                 deleteResponse.addProperty("id", messageId);
	                 session.getBasicRemote().sendText(deleteResponse.toString());
	             }
	    	}
	        
	    } catch (Exception e) {
	        
	        e.printStackTrace();
	        userSession.getBasicRemote().sendText("Error processing message: " + e.getMessage());
	        return;
	    }
	    
	}



	@OnClose
	public void handleClose(Session session) {
        String room = (String) session.getUserProperties().get("room");
        rooms.get(room).remove(session);
        System.out.println("Session closed and removed from room: " + room);
    }

	@OnError
	public void handleError(Throwable t) {
		t.printStackTrace();
	}
    
	
	
	
}