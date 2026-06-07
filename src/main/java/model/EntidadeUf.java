package model;

/**
 * Classe base abstrata da hierarquia de modelos (Herança).
 *
 * Generaliza o atributo comum a todas as entidades ligadas a um estado: a UF.
 * São subclasses diretas: {@link Municipio}, {@link IndicadorSeguranca} e a
 * abstrata {@link EntidadeMunicipal}.
 */
public abstract class EntidadeUf {

    protected String uf;

    protected EntidadeUf() {
    }

    protected EntidadeUf(String uf) {
        this.uf = uf;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}
