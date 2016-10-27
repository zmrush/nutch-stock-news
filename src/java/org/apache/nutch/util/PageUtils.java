package org.apache.nutch.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by mingzhu7 on 2016/10/27.
 */
public class PageUtils {
    public static String pasteAll(Node node){
        StringBuffer sb=new StringBuffer();
        if(node.getNodeType()==Node.ELEMENT_NODE && !"img".equalsIgnoreCase(node.getNodeName())){
            sb.append("<"+node.getNodeName().toLowerCase()+">");
        }else if(node.getNodeType()==Node.ELEMENT_NODE && "img".equalsIgnoreCase(node.getNodeName())){
            sb.append("<img src=\"");
            sb.append(node.getAttributes().getNamedItem("src").getNodeValue());
            sb.append("\"/>");
            return sb.toString();
        }
        else if(node.getNodeType()==Node.TEXT_NODE){
            sb.append(node.getTextContent().replace("\\s+", " ").trim());
            return sb.toString();
        }else{
            return "";
        }
        NodeList nodeList=node.getChildNodes();
        for(int i=0;i<nodeList.getLength();i++){
            sb.append(pasteAll(nodeList.item(i)));
        }
        sb.append("</"+node.getNodeName().toLowerCase()+">");
        return sb.toString();
    }
}
