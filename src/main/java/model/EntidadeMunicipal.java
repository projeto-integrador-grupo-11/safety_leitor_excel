package model;

/**
 * Especialização de {@link EntidadeUf} para entidades ligadas a um município
 * (Herança intermediária).
 *
 * Acrescenta o nome do município, atributo compartilhado por
 * {@link OcorrenciaSeguranca} e {@link PopulacaoMunicipio}.
 */
public abstract class EntidadeMunicipal extends EntidadeUf {

    protected String nomeMunicipio;

    protected EntidadeMunicipal() {
    }

    protected EntidadeMunicipal(String uf, String nomeMunicipio) {
        super(uf);
        this.nomeMunicipio = nomeMunicipio;
    }

    public String getNomeMunicipio() {
        return nomeMunicipio;
    }

    public void setNomeMunicipio(String nomeMunicipio) {
        this.nomeMunicipio = nomeMunicipio;
    }
}
