package com.sean.supermarketmealplanner.nutrition.application;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NutritionNameNormalizer {
    private static final Pattern PACKAGE = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\s*(?:kg|g|l|ml|cl|ud|uds)\\b");
    private static final Set<String> STOP = Set.of("de","del","la","el","los","las","con","para","marca");
    private static final Set<String> PROTECTED = Set.of("sin lactosa","integral","light","alto en proteina","sin gluten");
    public String normalize(String value) {
        if(value==null)return "";
        String result=Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
        result=PACKAGE.matcher(result).replaceAll(" ").replaceAll("\\s+"," ").trim();
        var tokens=new ArrayList<String>();
        for(String token:result.split(" ")) if(!STOP.contains(token)) tokens.add(token);
        return String.join(" ",tokens);
    }
    public boolean keepsProtectedMeaning(String original,String candidate){
        String a=normalize(original),b=normalize(candidate);
        return PROTECTED.stream().allMatch(phrase->!a.contains(phrase)||b.contains(phrase));
    }
}
