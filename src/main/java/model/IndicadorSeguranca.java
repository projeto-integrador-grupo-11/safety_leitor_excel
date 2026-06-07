package model;

public class IndicadorSeguranca extends EntidadeUf {

    private String tipo;
    private int ano;
    private int quantidade;

    public IndicadorSeguranca() {
    }

    public IndicadorSeguranca(String uf, String tipo, int ano, int quantidade) {
        super(uf);
        this.tipo = tipo;
        this.ano = ano;
        this.quantidade = quantidade;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    @Override
    public String toString() {
        return "IndicadorSeguranca{" +
                "uf='" + uf + '\'' +
                ", tipo='" + tipo + '\'' +
                ", ano=" + ano +
                ", quantidade=" + quantidade +
                '}';
    }
}
