import model.Municipio;
import repository.MunicipioRepository;
import service.LeitorExcel;


import java.util.List;

public class Main {

    public static void main(String[] args) {

        LeitorExcel leitor = new LeitorExcel();
        MunicipioRepository repo = new MunicipioRepository();

        List<Municipio> municipios = leitor.ler("src/main/resources/data_municipio.xlsx");

        repo.salvarLista(municipios);
        
        System.out.println("Processo finalizado!");
    }
}