package service;

import model.Municipio;
import model.Pessoa;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import repository.LogRepository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LeitorExcel {
    private LogRepository logRepo = new LogRepository();
    private static final Logger log = LogManager.getLogger(LeitorExcel.class);

    public List<Municipio> ler(String caminhoArquivo) {

        List<Municipio> municipios = new ArrayList<>();
        long inicio = System.currentTimeMillis();


        log.info("Iniciando leitura do arquivo: {}", caminhoArquivo);
        logRepo.salvar("INFO","Iniciando leitura do arquivo");
        try (
                InputStream arquivo = new FileInputStream(caminhoArquivo);
                Workbook workbook = new XSSFWorkbook(arquivo)
        ) {

            Sheet sheet = workbook.getSheetAt(0);
            int contador = 0;
            for (Row row : sheet) {

                // pula cabeçalho (linha 0)
                if (row.getRowNum() == 0) {
                    continue;
                }

                try {
                    String nome = row.getCell(0).getStringCellValue();
                    Double idhmGeral = row.getCell(2).getNumericCellValue();
                    Double renda = row.getCell(4).getNumericCellValue();
                    Double educacao = row.getCell(6).getNumericCellValue();
                    Double longevidade = row.getCell(8).getNumericCellValue();


                    Municipio municipio = new Municipio(nome, idhmGeral, renda, educacao, longevidade);

                    municipios.add(municipio);
                    contador++;
                    log.info("Linha {} processada: {}", row.getRowNum(), municipio);

                } catch (Exception e) {
                    log.warn("Erro ao processar linha {}: {}", row.getRowNum(), e.getMessage());
                    logRepo.salvar("WARN", "Erro ao processar a linha atual");

                }
            }

           /* long fim = System.currentTimeMillis();*/

            log.info("Leitura finalizada. Total de registros: {}", municipios.size());
                logRepo.salvar("INFO","Leitura finalzada!Total de registros: " + contador);
            contador = 0;
/*
            log.info("Tempo de execução: {} ms", (fim - inicio));

*/
            System.out.println("------------------------------------------------------------------------");
        } catch (Exception e) {
            log.error("Erro ao abrir arquivo: {}", e.getMessage());
            logRepo.salvar("ERROR", "Erro ao abrir arquivo: " + e.getMessage());

        }

        return municipios;
    }
}
