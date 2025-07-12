-injars build/libs/TeraxPay.jar
-outjars build/libs/TeraxPay-obfuscated.jar

# Obfuscação completa e agressiva, mas sem remover nada
-dontoptimize
-dontshrink
-overloadaggressively
-dontpreverify
-dontwarn
-ignorewarnings
-allowaccessmodification

# Reempacota tudo para dificultar engenharia reversa
-repackageclasses ''
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-methodobfuscationdictionary proguard-dict.txt

# Classe principal do plugin
-keep public class world.terax.pay.TeraxPay {
    public void onEnable();
    public void onDisable();
}

# Garante que comandos e listeners do Bukkit não sejam removidos
-keep class org.bukkit.** { *; }
-keep class net.minecraft.** { *; }

# Redis realocado
-keep class world.terax.libs.redis.** { *; }

# Protege atributos úteis e plugin.yml
-keepattributes Signature,InnerClasses,EnclosingMethod,Deprecated,SourceFile,LineNumberTable,*Annotation*

# Lombok
-keep class lombok.** { *; }
