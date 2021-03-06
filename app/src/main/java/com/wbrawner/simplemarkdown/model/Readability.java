package com.wbrawner.simplemarkdown.model;

import java.util.ArrayList;
import java.util.List;

public class Readability {
    private String content = "";
    private final String DELIMS = ".!?\n";

    public Readability(String content){
        this.content = content;
    }

    public List<Sentence> sentences(){

        ArrayList<Sentence> list = new ArrayList<>();

        int startOfSentance = 0;
        String line = "";
        for(int i = 0; i < content.length(); i++){
            String c = content.charAt(i) + "";
            if(DELIMS.contains(c)){
                list.add(new Sentence(content,startOfSentance,i));
                startOfSentance = i + 1;
                line = "";
            }else{
                line += c;
            }
        }
        if(line != "")list.add(new Sentence(content,startOfSentance,content.length()));

        return list;
    }
}
