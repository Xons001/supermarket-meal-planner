package com.sean.supermarketmealplanner.nutrition.application;

import com.sean.supermarketmealplanner.catalog.infrastructure.persistence.ProductEntity;
import com.sean.supermarketmealplanner.nutrition.application.port.ExternalNutritionCandidate;
import com.sean.supermarketmealplanner.nutrition.domain.NutritionEnums.MatchMethod;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class NutritionMatchScorer {
    private final NutritionNameNormalizer normalizer;
    public NutritionMatchScorer(NutritionNameNormalizer normalizer){this.normalizer=normalizer;}
    public Result score(ProductEntity product,ExternalNutritionCandidate candidate){
        String expected=normalizer.normalize(product.getName()),actual=normalizer.normalize(candidate.name());
        double barcode=Objects.equals(clean(product.getBarcode()),clean(candidate.barcode()))&&clean(product.getBarcode())!=null?100:0;
        double name=similarity(expected,actual)*100;
        double brand=blank(product.getBrand())||blank(candidate.brand())?50:
                similarity(normalizer.normalize(product.getBrand()),normalizer.normalize(candidate.brand()))*100;
        double pack=packageScore(product,candidate);
        double category=blank(candidate.category())?50:(normalizer.normalize(candidate.category()).contains(
                normalizer.normalize(product.getCategory().getName()))?100:50);
        double measurement=measurementScore(product,candidate);
        double completeness=completeness(candidate);
        double total=barcode>0 ? 55+name*.15+brand*.10+pack*.05+category*.05+measurement*.05+completeness*.05
                : name*.50+brand*.15+pack*.10+category*.10+measurement*.05+completeness*.10;
        if(!normalizer.keepsProtectedMeaning(product.getName(),candidate.name()))total=Math.min(total,74.99);
        MatchMethod method=barcode>0?MatchMethod.BARCODE_EXACT:name>=99?MatchMethod.NAME_EXACT:
                name>=85&&brand>=80?MatchMethod.NAME_BRAND:MatchMethod.FUZZY_NAME;
        var breakdown=new LinkedHashMap<String,BigDecimal>();
        breakdown.put("barcode",round(barcode));breakdown.put("name",round(name));breakdown.put("brand",round(brand));
        breakdown.put("packageSize",round(pack));breakdown.put("category",round(category));
        breakdown.put("measurementType",round(measurement));breakdown.put("nutritionCompleteness",round(completeness));
        return new Result(round(Math.min(100,total)),method,breakdown);
    }
    private double packageScore(ProductEntity p,ExternalNutritionCandidate c){
        if(c.packageQuantity()==null)return 50;
        if(p.getPackageQuantity()==null)return 50;
        var max=p.getPackageQuantity().max(c.packageQuantity()); if(max.signum()==0)return 100;
        return Math.max(0,100-p.getPackageQuantity().subtract(c.packageQuantity()).abs().divide(max,4,RoundingMode.HALF_UP).doubleValue()*100);
    }
    private double measurementScore(ProductEntity p,ExternalNutritionCandidate c){
        if(c.packageUnit()==null)return 50; String u=c.packageUnit().toUpperCase(Locale.ROOT);
        return switch(p.getMeasurementType()){case WEIGHT->u.matches("G|KG")?100:0;case VOLUME->u.matches("ML|CL|L")?100:0;case UNIT->u.matches("UNIT|UNITS|UD|UDS")?100:50;};
    }
    private double completeness(ExternalNutritionCandidate c){var n=c.nutrition();int count=0;
        if(n.caloriesPer100g()!=null)count++;if(n.proteinPer100g()!=null)count++;if(n.carbohydratesPer100g()!=null)count++;if(n.fatPer100g()!=null)count++;
        if(n.fiberPer100g()!=null)count++;if(n.sugarPer100g()!=null)count++;if(n.saltPer100g()!=null)count++;if(n.saturatedFatPer100g()!=null)count++;
        return count*12.5;}
    private double similarity(String a,String b){if(a.equals(b))return 1;if(a.isBlank()||b.isBlank())return 0;
        var left=new HashSet<>(List.of(a.split(" ")));var right=new HashSet<>(List.of(b.split(" ")));
        var union=new HashSet<>(left);union.addAll(right);var intersection=new HashSet<>(left);intersection.retainAll(right);
        double token=union.isEmpty()?0:(double)intersection.size()/union.size();
        int distance=levenshtein(a,b);double edit=1-(double)distance/Math.max(a.length(),b.length());return token*.6+edit*.4;}
    private int levenshtein(String a,String b){int[] previous=new int[b.length()+1];for(int j=0;j<=b.length();j++)previous[j]=j;
        for(int i=1;i<=a.length();i++){int[] current=new int[b.length()+1];current[0]=i;for(int j=1;j<=b.length();j++)current[j]=Math.min(Math.min(current[j-1]+1,previous[j]+1),previous[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));previous=current;}return previous[b.length()];}
    private String clean(String v){return blank(v)?null:v.replaceAll("[^A-Za-z0-9]","").toUpperCase(Locale.ROOT);}private boolean blank(String v){return v==null||v.isBlank();}
    private BigDecimal round(double value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP);}
    public record Result(BigDecimal confidence,MatchMethod method,Map<String,BigDecimal> breakdown){}
}
