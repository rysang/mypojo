package erwins.util.tools



import static org.junit.Assert.*
import groovy.util.slurpersupport.NodeChild

import org.cyberneko.html.parsers.SAXParser

class GroovyXml {
	
	def url
	def encode = 'UTF-8'
	def xml
	def text
	def current
	
	public build(){
		text = new URL(url).openStream().getText(encode)
		xml = new XmlSlurper(new SAXParser()).parseText(text)
		return this
	}
	
	/** 텍스트가 들어있는 경로를 찾는다. 데이터 파싱의 사전조사로 사용된다. 
	 * 아오 신발 equals()가 잘못 정의되어있는지 ==가 text() 기준인듯 하다. */
	public findText(text){
		def result
		parseEach xml , {
			if(it.text()==text){
				def node = it
				def list = []
				while(node.name() != 'HTML'){ //ㅅㅂ 이게 한계
				//while(node != node.parent()){
					def name = node.name()
					def index = node.parent()[name].findIndexOf { it == node }
					list << "$name[$index]"
					node  = node.parent()
				}
				result = list.reverse().join('.') 
			}
		}
		return result
	}
	
	/** 모든 node를 돌면서 클로저를 실행한다.
	 * node.children()을 직접 얻으려고 하면 오류난다. node.children().szie()는 괜찮다. */
	public void parseEach(NodeChild node,c){
		if( node.children().size()==0) c(node)
		else node.children().each { c(node); parseEach it,c } 
	}
}
