package model;

public class PopulacaoMunicipio {

    private String uf;
    private String nomeMunicipio;
    private int populacao;
    private int ano;

    public PopulacaoMunicipio() {
    }

    public PopulacaoMunicipio(String uf, String nomeMunicipio, int populacao, int ano) {
        this.uf = uf;
        this.nomeMunicipio = nomeMunicipio;
        this.populacao = populacao;
        this.ano = ano;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getNomeMunicipio() {
        return nomeMunicipio;
    }

    public void setNomeMunicipio(String nomeMunicipio) {
        this.nomeMunicipio = nomeMunicipio;
    }

    public int getPopulacao() {
        return populacao;
    }

    public void setPopulacao(int populacao) {
        this.populacao = populacao;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    @Override
    public String toString() {
        return "PopulacaoMunicipio{" +
                "uf='" + uf + '\'' +
                ", nomeMunicipio='" + nomeMunicipio + '\'' +
                ", populacao=" + populacao +
                ", ano=" + ano +
                '}';
    }
}
