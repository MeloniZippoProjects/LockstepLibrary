package xeviousvs;

public class StatisticheUtente {

    public int numeroVittorie;
    public int numeroSconfitte;
    public double percentualeVittorie;

    public StatisticheUtente(int numeroVittorie, int numeroSconfitte) {
        this.numeroVittorie = numeroVittorie;
        this.numeroSconfitte = numeroSconfitte;

        double numeroPartite = numeroVittorie + numeroSconfitte;
        this.percentualeVittorie = (numeroPartite == 0) ? 0 : (numeroVittorie / numeroPartite) * 100;
    }
}
