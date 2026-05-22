package model;

public class Municipio {

    private Long id;
    private String uf;
    private String nome;
    private Double idhmGeral;
    private Double renda;
    private Double educacao;
    private Double longevidade;

    public Municipio() {
    }

    public Municipio(String uf, String nome, Double idhmGeral, Double renda, Double educacao, Double longevidade) {
        this.uf = uf;
        this.educacao = educacao;
        this.idhmGeral = idhmGeral;
        this.longevidade = longevidade;
        this.nome = nome;
        this.renda = renda;
    }

    public Municipio(String nome, Double idhmGeral, Double renda, Double educacao, Double longevidade) {
        this("SP", nome, idhmGeral, renda, educacao, longevidade);
    }


    public Municipio(String nome) {
        this.nome = nome;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public Double getEducacao() {
        return educacao;
    }

    public void setEducacao(Double educacao) {
        this.educacao = educacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getIdhmGeral() {
        return idhmGeral;
    }

    public void setIdhmGeral(Double idhmGeral) {
        this.idhmGeral = idhmGeral;
    }

    public Double getLongevidade() {
        return longevidade;
    }

    public void setLongevidade(Double longevidade) {
        this.longevidade = longevidade;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getRenda() {
        return renda;
    }

    public void setRenda(Double renda) {
        this.renda = renda;
    }

    @Override
    public String toString() {
        return "municipio{" +
                "uf='" + uf + '\'' +
                ", educacao=" + educacao +
                ", id=" + id +
                ", nome='" + nome + '\'' +
                ", idhmGeral=" + idhmGeral +
                ", renda=" + renda +
                ", longevidade=" + longevidade +
                '}';
    }
}
