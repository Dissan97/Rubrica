package it.dissanahmed.rubrica.ex;

import org.jetbrains.annotations.NotNull;

public class PersonaException extends Exception{


        public PersonaException(@NotNull ExceptionType personaExceptionType){
                this(personaExceptionType, "");
        }

        public PersonaException(@NotNull ExceptionType personaExceptionType, String suffix){
                super(personaExceptionType.message + " " + suffix);
        }

        public static enum ExceptionType{
                INVALID_ENTRY("C'è una entry non valida"),
                WRONG_PHONE_NUMBER("il numero di telefono non è valido"),
                EMPTY_FIELD("Non possono esserci entry vuote"),
                WRONG_ADDRESS("L'indirizzo non è valido"),
                ALREADY_EXIST("Esiste un'altra persona con il numero di telefono:"),
                NOT_EXISTS("Non esiste la persona:");

                private String message;
                ExceptionType(String message) {
                        this.message = message;
                }
        }
}
