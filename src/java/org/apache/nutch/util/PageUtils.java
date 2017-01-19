package org.apache.nutch.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by mingzhu7 on 2016/10/28.
 */
public class PageUtils {
    public static void main(String[] args){
        String url= "http://news.xinhuanet.com/fortune/2017-01/10/c_129438935.htm";
        String initial="129438935_14840053765661n.jpg";
        System.out.println(parseImgSrc(initial,url));

    }
    public static String parseImgSrc(String initial,String url){
        if(initial.startsWith("http://") || initial.startsWith("https://"))
            return initial;
        if(!url.endsWith("/"))
            url=url.substring(0,url.lastIndexOf("/")+1);
        if(initial.startsWith("/")){
            String protocal=url.substring(0,url.indexOf("://")+3);
            String host=url.substring(url.indexOf("://")+3);
            host=host.substring(0,host.indexOf("/"));
            return protocal+host+initial;
        }else if(initial.startsWith("./")){
            String newInitial=initial.substring(initial.indexOf("./")+2);
            return parseImgSrc(newInitial,url);
        }else if(initial.startsWith("../")){
            String newInitial=initial.substring(initial.indexOf("../")+3);
            url=url.substring(0,url.length()-1);
            url=url.substring(0,url.lastIndexOf("/")+1);
            return parseImgSrc(newInitial,url);
        }else{
            return url+initial;
        }
    }
    public static String pasteAll(Node node,String url){
        StringBuffer sb=new StringBuffer();
        if(node.getNodeType()==Node.ELEMENT_NODE && !"img".equalsIgnoreCase(node.getNodeName())){
            sb.append("<"+node.getNodeName().toLowerCase()+">");
        }else if(node.getNodeType()==Node.ELEMENT_NODE && "img".equalsIgnoreCase(node.getNodeName())){
            sb.append("<img src=\"");
            try{
                sb.append(parseImgSrc(node.getAttributes().getNamedItem("src").getNodeValue(),url));
            }catch (Exception e){
                sb.append(node.getAttributes().getNamedItem("src").getNodeValue());
            }
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
            sb.append(pasteAll(nodeList.item(i),url));
        }
        sb.append("</"+node.getNodeName().toLowerCase()+">");
        return sb.toString();

    }
}
