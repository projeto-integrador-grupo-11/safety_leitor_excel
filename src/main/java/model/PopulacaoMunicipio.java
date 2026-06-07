package model;

public class PopulacaoMunicipio extends EntidadeMunicipal {

    private int populacao;
    private int ano;

    public PopulacaoMunicipio() {
    }

    public PopulacaoMunicipio(String uf, String nomeMunicipio, int populacao, int ano) {
        super(uf, nomeMunicipio);
        this.populacao = populacao;
        this.ano = ano;
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
