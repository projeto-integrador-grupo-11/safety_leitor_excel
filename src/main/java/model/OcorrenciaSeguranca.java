package model;

public class OcorrenciaSeguranca extends EntidadeMunicipal {

    private String evento;
    private int anoRef;
    private int mesRef;
    private int qtdVitimas;

    public OcorrenciaSeguranca() {
    }

    public OcorrenciaSeguranca(String uf, String nomeMunicipio, String evento,
                               int anoRef, int mesRef, int qtdVitimas) {
        super(uf, nomeMunicipio);
        this.evento = evento;
        this.anoRef = anoRef;
        this.mesRef = mesRef;
        this.qtdVitimas = qtdVitimas;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public int getAnoRef() {
        return anoRef;
    }

    public void setAnoRef(int anoRef) {
        this.anoRef = anoRef;
    }

    public int getMesRef() {
        return mesRef;
    }

    public void setMesRef(int mesRef) {
        this.mesRef = mesRef;
    }

    public int getQtdVitimas() {
        return qtdVitimas;
    }

    public void setQtdVitimas(int qtdVitimas) {
        this.qtdVitimas = qtdVitimas;
    }

    @Override
    public String toString() {
        return "OcorrenciaSeguranca{" +
                "uf='" + uf + '\'' +
                ", nomeMunicipio='" + nomeMunicipio + '\'' +
                ", evento='" + evento + '\'' +
                ", anoRef=" + anoRef +
                ", mesRef=" + mesRef +
                ", qtdVitimas=" + qtdVitimas +
                '}';
    }
}
