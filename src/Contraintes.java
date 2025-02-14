package framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Parameter;
public class Contraintes {

    // Méthode qui applique toutes les contraintes sur l'objet
    public static List<ResponseValidation> valider(Object obj, Annotation[] annotatsParam,String input) {
        List<ResponseValidation> result = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();
    
        // Vérification des contraintes sur le paramètre
        for (Annotation annotation : annotatsParam) {
            if (annotation instanceof framework.Annotation.NotNull notnull) {
                if (obj == null || obj.equals("")) {
                    erreurs.add(getMessage(notnull));
                }
            }
    
            if (annotation instanceof framework.Annotation.Size sizeConstraint) {
                if (!isValidSize(obj, sizeConstraint)) {
                    erreurs.add(getMessage(sizeConstraint));
                }
            }
    
            if (annotation instanceof framework.Annotation.Min minConstraint) {
                if (!isValidMin(obj, minConstraint)) {
                    erreurs.add(getMessage(minConstraint));
                }
            }
    
            if (annotation instanceof framework.Annotation.Max maxConstraint) {
                if (!isValidMax(obj, maxConstraint)) {
                    erreurs.add(getMessage(maxConstraint));
                }
            }
    
            if (annotation instanceof framework.Annotation.Email email) {
                if (!isValidEmail(obj)) {
                    erreurs.add(getMessage(email));
                }
            }
    
            if (annotation instanceof framework.Annotation.Pattern patternConstraint) {
                if (!isValidPattern(obj, patternConstraint)) {
                    erreurs.add(getMessage(patternConstraint));
                }
            }
    
            if (annotation instanceof framework.Annotation.Valid) {
                result.add(Contraintes.validerAttr(obj,input));
            }
        }
        result.add(new ResponseValidation(input,erreurs,obj));
        return result;
    }
    
    public static List<ResponseValidation> validateObject(Object obj) {
        List<ResponseValidation> errors = new ArrayList<>();
        
        try {
            Class<?> objClass = obj.getClass();
            Field[] fields = objClass.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);
                errors.addAll(Contraintes.valider(fieldValue, field.getAnnotations(),field.getName()));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
        return errors;
    }

    public static ResponseValidation validerAttr(Object obj, String inputName) {
        List<ResponseValidation> errors = validateObject(obj);
        
        List<String> combinedErrors = new ArrayList<>();
        Object value = null;
        
        for (ResponseValidation validation : errors) {
            if (!validation.getErrors().isEmpty()) {
                combinedErrors.addAll(validation.getErrors());
                if (value == null) {
                    value = validation.getValue();
                }
            }
        }
        
        // Créer et retourner une ResponseValidation avec les erreurs combinées
        return new ResponseValidation(inputName, combinedErrors, value);
    }
    // Méthode pour vérifier la taille (uniquement pour les String)
    private static boolean isValidSize(Object obj, framework.Annotation.Size constraint) {
        if (obj instanceof String) {
            String value = (String) obj;
            int length = value != null ? value.length() : 0;
            return length >= constraint.min() && length <= constraint.max();
        }
        return false;
    }

    // Méthode pour vérifier la contrainte Min
    private static boolean isValidMin(Object obj, framework.Annotation.Min constraint) {
        if (obj == null) return false;
        
        if (obj instanceof Integer) {
            return (Integer) obj >= constraint.value();
        } else if (obj instanceof Long) {
            return (Long) obj >= constraint.value();
        } else if (obj instanceof Double) {
            return (Double) obj >= constraint.value();
        } else if (obj instanceof Float) {
            return (Float) obj >= constraint.value();
        }
        return false;
    }

    // Méthode pour vérifier la contrainte Max
    private static boolean isValidMax(Object obj, framework.Annotation.Max constraint) {
        if (obj == null) return false;
        
        if (obj instanceof Integer) {
            return (Integer) obj <= constraint.value();
        } else if (obj instanceof Long) {
            return (Long) obj <= constraint.value();
        } else if (obj instanceof Double) {
            return (Double) obj <= constraint.value();
        } else if (obj instanceof Float) {
            return (Float) obj <= constraint.value();
        }
        return false;
    }

    // Méthode pour vérifier la validité d'un email
    private static boolean isValidEmail(Object obj) {
        if (obj instanceof String) {
            String value = (String) obj;
            return value != null && value.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        }
        return false;
    }

    // Méthode pour vérifier la conformité avec un pattern
    private static boolean isValidPattern(Object obj, framework.Annotation.Pattern constraint) {
        if (obj instanceof String) {
            String value = (String) obj;
            return value != null && value.matches(constraint.regexp());
        }
        return false;
    }

    private static String getMessage(Annotation annotation) {
        if (annotation instanceof framework.Annotation.NotNull notnull) {
            return notnull.message();
        } else if (annotation instanceof framework.Annotation.Size size) {
            return size.message();
        } else if (annotation instanceof framework.Annotation.Min min) {
            return min.message();
        } else if (annotation instanceof framework.Annotation.Max max) {
            return max.message();
        } else if (annotation instanceof framework.Annotation.Email email) {
            return email.message();
        } else if (annotation instanceof framework.Annotation.Pattern pattern) {
            return pattern.message();
        }
        return "";
    }
}