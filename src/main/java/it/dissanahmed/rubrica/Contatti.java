package it.dissanahmed.rubrica;

import it.dissanahmed.rubrica.ex.PersonaException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Contatti {

        private List<Persona> contatti;

        public Contatti(List<Persona> contatti) {
                this.contatti = contatti;
        }

        public Contatti() {
                this.contatti = new ArrayList<>();
        }


        public void addPersona(@NotNull Persona persona) throws PersonaException {
                if (isPersonaIn(persona)){
                        throw new PersonaException(PersonaException.ExceptionType.ALREADY_EXIST, persona.getTelefono());
                }
                contatti.add(persona);
        }

        public boolean isPersonaIn(Persona persona){
                return contatti.contains(persona);
        }

        public List<Persona> getContatti() {
                return contatti;
        }

}
