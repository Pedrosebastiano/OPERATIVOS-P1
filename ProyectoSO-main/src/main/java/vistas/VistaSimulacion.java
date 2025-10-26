package vistas;

import java.awt.BorderLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import org.jfree.chart.plot.PiePlot;
import micelaneos.*;
import modelos.*;
import java.util.Random;

public class VistaSimulacion extends javax.swing.JFrame {
    DefaultPieDataset dataset1;
    CPU cpu;
    Reloj reloj;
    List<Proceso> listolista;
    List<Proceso> todos;
    PerformanceMetrics metrics;
    Planificador planificador;
    private int relojGlobal;
    private ChartPanel chartPanel;
    private boolean chartVisible = false;

    public VistaSimulacion(int tiempo, int politica, List listo, List todos) {
        initComponents();
        dataset1 = new DefaultPieDataset();
        
        chartPanel = createPieChart(dataset1, "Utilización del CPU");
        chartPanel.setPreferredSize(new Dimension(700, 400));
        
        this.politica.setSelectedIndex(politica);
        this.tiempoinstruccion.setValue(tiempo);
        this.jLabel16.setText(tiempo + " ms");
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.listolista = listo;
        this.todos = todos;
        this.relojGlobal = 0;
        this.uPcbs();
        
        // Thread to update metrics and event log
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    if (metrics != null) {
                        updateMetricsDisplay();
                    }
                    if (planificador != null) {
                        updateEventLogDisplay();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public CPU getCpu() {
        return cpu;
    }

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
    }

    public Reloj getReloj() {
        return reloj;
    }

    public void setReloj(Reloj reloj) {
        this.reloj = reloj;
    }
    
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }
    
    public void setPlanificador(Planificador planificador) {
        this.planificador = planificador;
    }
    
    public int getRelojGlobal() {
        return relojGlobal;
    }

    public VistaSimulacion(){
        initComponents();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    private boolean validateInputs() {
        try {
            if (nombre.getText().trim().isEmpty() || duracion.getText().trim().isEmpty()) {
                return false;
            }
            Integer.parseInt(duracion.getText().trim());
            if (this.tipoproceso.getSelectedIndex() == 1) {
                if (cicloexcep.getText().trim().isEmpty() || duracionexcep.getText().trim().isEmpty()) {
                    return false;
                }
                Integer.parseInt(cicloexcep.getText().trim());
                Integer.parseInt(duracionexcep.getText().trim());
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void limpiarCampos() {
        nombre.setText("");
        duracion.setText("");
        cicloexcep.setText("");
        duracionexcep.setText("");
        tipoproceso.setSelectedIndex(0);
        prioridadField.setText("");
    }

    public void setCPU(String t){
       this.cpu1.setText(t);
    }

    public void uPcbs(){
        String d = "";
        Nodo p = todos.getHead();
        while(p != null){
           if (planificador != null) {
                d += planificador.stringInterfaz((Proceso) p.getValue());
            }
            p = p.getpNext();
        }
        this.setPcbs(d);
    }

    public void setReloj(String t){
        this.relojglobal.setText(t);
        try {
            this.relojGlobal = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            this.relojGlobal = 0;
        }
    }

    public void setListos(String t) {
        this.listos.setText(t);
    }

    public void setBloqueados(String t) {
        this.bloqueados.setText(t);
    }

    public void setSalida(String t) {
        this.salida.setText(t);
    }

    public void setPcbs(String t){
        this.pcbs.setText(t);
    }

    public void setTiempoInstruccion(String i){
        this.relojglobal.setText(i);
    }

    public int getTiempoInstrucion(){
        return this.tiempoinstruccion.getValue();
    }

    public int getPolitica(){
        return this.politica.getSelectedIndex();
    }

    private ChartPanel createPieChart(DefaultPieDataset dataset, String title) {
        dataset.setValue("Usuario", 0);
        dataset.setValue("Sistema Operativo", 0);

        JFreeChart pieChart = ChartFactory.createPieChart(
                title,
                dataset,
                true, true, false);
        PiePlot plot = (PiePlot) pieChart.getPlot();
        plot.setSectionPaint("Usuario", Color.BLUE);
        plot.setSectionPaint("Sistema Operativo", Color.RED);
        
        ChartPanel chartPanel = new ChartPanel(pieChart);
        return chartPanel;
    }

    public void updateDataset(int chartNumber, String category, int value) {
        SwingUtilities.invokeLater(() -> {
            Number existingValue = dataset1.getValue(category);
            int newValue = (existingValue == null ? 0 : existingValue.intValue()) + value;
            dataset1.setValue(category, newValue);
            if (chartPanel != null) {
                chartPanel.repaint();
            }
        });
    }

    /**
     * NEW: Update suspended ready list display
     */
    public void setListosSuspendidosText(String t) {
        SwingUtilities.invokeLater(() -> {
            this.listosSuspendidos.setText(t);
        });
    }

    /**
     * NEW: Update suspended blocked list display
     */
    public void setBloqueadosSuspendidosText(String t) {
        SwingUtilities.invokeLater(() -> {
            this.bloqueadosSuspendidos.setText(t);
        });
    }
    
    public void updateMetrics(String metricsText) {
        SwingUtilities.invokeLater(() -> {
            this.metricsArea.setText(metricsText);
        });
    }
    
    public void updateEventLog(String logText) {
        SwingUtilities.invokeLater(() -> {
            this.eventLogArea.setText(logText);
            // Auto-scroll to bottom
            this.eventLogArea.setCaretPosition(this.eventLogArea.getDocument().getLength());
        });
    }
    
    private void updateMetricsDisplay() {
        if (metrics != null) {
            String metricsText = metrics.getMetricsString();
            if (planificador != null) {
                double fairness = metrics.getFairness(todos);
                metricsText += String.format("\nEquidad: %.2f", fairness);
                
                if (planificador.getMemoryManager() != null) {
                    metricsText += String.format("\n\nMemoria Disponible: %d MB", 
                        planificador.getMemoryManager().getAvailableMemory());
                    metricsText += String.format("\nMemoria Total: %d MB", 
                        planificador.getMemoryManager().getTotalMemory());
                    metricsText += String.format("\nUtilización de Memoria: %.2f%%", 
                        planificador.getMemoryManager().getMemoryUtilization());
                }
            }
            updateMetrics(metricsText);
        }
    }
    
    private void updateEventLogDisplay() {
        if (planificador != null && planificador.getLogger() != null) {
            String log = planificador.getLogger().getEventsAsString();
            updateEventLog(log);
        }
    }
    
    private void toggleChart() {
        if (!chartVisible) {
            chartContainerPanel.removeAll();
            chartContainerPanel.setLayout(new BorderLayout());
            chartContainerPanel.add(chartPanel, BorderLayout.CENTER);
            chartContainerPanel.revalidate();
            chartContainerPanel.repaint();
            toggleChartButton.setText("Ocultar Gráfico");
            chartVisible = true;
        } else {
            chartContainerPanel.removeAll();
            chartContainerPanel.revalidate();
            chartContainerPanel.repaint();
            toggleChartButton.setText("Mostrar Gráfico de CPU");
            chartVisible = false;
        }
    }
    
    private void generarProcesosAleatorios() {
        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            int id = todos.getSize();
            String nombreProceso = "Proceso " + id;

            int instrucciones = 20 + rand.nextInt(180); // 20-200 instructions
            boolean isIOBound = rand.nextBoolean();
            String tipo = isIOBound ? "I/O Bound" : "CPU Bound";

            int ciclosExcepcion = isIOBound ? (5 + rand.nextInt(20)) : 1;
            int duracionExcepcion = isIOBound ? (3 + rand.nextInt(10)) : 1;
            
            // Random priority between 0 (highest) and 4 (lowest)
            int prioridad = rand.nextInt(5);

            Proceso p = new Proceso(
                id,
                nombreProceso,
                tipo,
                instrucciones,
                ciclosExcepcion,
                duracionExcepcion,
                prioridad
            );

            listolista.appendLast(p);
            todos.appendLast(p);
        }

        this.uPcbs();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code - UI Components">                          
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        guardarproceso = new javax.swing.JButton();
        nombre = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        duracion = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        cicloexcep = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        duracionexcep = new javax.swing.JTextField();
        tipoproceso = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        generarAleatoriosBtn = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        prioridadField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        metricsArea = new javax.swing.JTextArea();
        toggleChartButton = new javax.swing.JButton();
        chartContainerPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        tiempoinstruccion = new javax.swing.JSlider();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        politica = new javax.swing.JComboBox<>();
        jLabel17 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        cpu1 = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        salida = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        pcbs = new javax.swing.JTextArea();
        jScrollPane7 = new javax.swing.JScrollPane();
        bloqueados = new javax.swing.JTextArea();
        jScrollPane6 = new javax.swing.JScrollPane();
        listos = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        relojglobal = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        bloqueadosSuspendidos = new javax.swing.JTextArea();
        jScrollPane9 = new javax.swing.JScrollPane();
        listosSuspendidos = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        eventLogArea = new javax.swing.JTextArea();
        clearLogBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        guardarproceso.setText("Añadir");
        guardarproceso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guardarprocesoActionPerformed(evt);
            }
        });
        jPanel2.add(guardarproceso, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 340, 140, 30));

        nombre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nombreActionPerformed(evt);
            }
        });
        jPanel2.add(nombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 70, 280, -1));

        jLabel9.setText("Nombre:");
        jPanel2.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 70, -1, -1));

        jLabel10.setText("Duración: ");
        jPanel2.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 110, -1, -1));

        duracion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duracionActionPerformed(evt);
            }
        });
        jPanel2.add(duracion, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 110, 280, -1));

        jLabel11.setText("Cada excepción:");
        jPanel2.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 210, -1, -1));

        cicloexcep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cicloexcepActionPerformed(evt);
            }
        });
        jPanel2.add(cicloexcep, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 210, 280, -1));

        jLabel12.setText("Duración excepción:");
        jPanel2.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 250, -1, -1));

        duracionexcep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duracionexcepActionPerformed(evt);
            }
        });
        jPanel2.add(duracionexcep, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 250, 280, -1));

        tipoproceso.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "CPU Bound", "I/O Bound" }));
        tipoproceso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tipoprocesoActionPerformed(evt);
            }
        });
        jPanel2.add(tipoproceso, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 160, 280, -1));

        jLabel13.setText("Tipo:");
        jPanel2.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 160, 30, 20));

        generarAleatoriosBtn.setText("Generar 10 Procesos Aleatorios");
        generarAleatoriosBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generarAleatoriosBtnActionPerformed(evt);
            }
        });
        jPanel2.add(generarAleatoriosBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 340, 200, 30));

        jLabel14.setText("Prioridad (0-4):");
        jPanel2.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 290, -1, -1));

        prioridadField.setText("0");
        jPanel2.add(prioridadField, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 290, 280, -1));

        jTabbedPane1.addTab("Añadir", jPanel2);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        metricsArea.setEditable(false);
        metricsArea.setColumns(20);
        metricsArea.setRows(5);
        metricsArea.setFont(new java.awt.Font("Monospaced", 0, 14));
        jScrollPane10.setViewportView(metricsArea);

        jPanel1.add(jScrollPane10, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 710, 200));

        toggleChartButton.setText("Mostrar Gráfico de CPU");
        toggleChartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleChartButtonActionPerformed(evt);
            }
        });
        jPanel1.add(toggleChartButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 230, 200, 30));

        chartContainerPanel.setLayout(new java.awt.BorderLayout());
        jPanel1.add(chartContainerPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 270, 710, 240));

        jTabbedPane1.addTab("Estadisticas", jPanel1);

        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tiempoinstruccion.setMaximum(5000);
        tiempoinstruccion.setMinimum(1);
        tiempoinstruccion.setValue(5000);
        tiempoinstruccion.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tiempoinstruccionStateChanged(evt);
            }
        });
        jPanel3.add(tiempoinstruccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 170, 220, -1));

        jLabel15.setText("Tiempo instrucción: ");
        jPanel3.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 170, -1, -1));

        jLabel16.setText("5000 ms");
        jPanel3.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 190, 110, -1));

        politica.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "FIFO", "RR", "SPN", "SRT", "HRRN", "Prioridad" }));
        politica.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                politicaItemStateChanged(evt);
            }
        });
        politica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                politicaActionPerformed(evt);
            }
        });
        jPanel3.add(politica, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 230, 210, -1));

        jLabel17.setText("Politica de Planificación: ");
        jPanel3.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 230, -1, -1));

        jButton2.setText("Iniciar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 270, 150, 30));

        jTabbedPane1.addTab("Configuración", jPanel3);

        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        cpu1.setEditable(false);
        cpu1.setColumns(20);
        cpu1.setRows(5);
        jScrollPane4.setViewportView(cpu1);

        jPanel4.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 320, 100, 170));

        salida.setEditable(false);
        salida.setColumns(20);
        salida.setRows(5);
        jScrollPane1.setViewportView(salida);

        jPanel4.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 20, 100, 210));

        pcbs.setEditable(false);
        pcbs.setColumns(20);
        pcbs.setRows(5);
        pcbs.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                pcbsCaretUpdate(evt);
            }
        });
        jScrollPane2.setViewportView(pcbs);

        jPanel4.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, 220, 480));

        bloqueados.setEditable(false);
        bloqueados.setColumns(20);
        bloqueados.setRows(5);
        jScrollPane7.setViewportView(bloqueados);

        jPanel4.add(jScrollPane7, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 20, 110, 480));

        listos.setEditable(false);
        listos.setColumns(20);
        listos.setRows(5);
        jScrollPane6.setViewportView(listos);

        jPanel4.add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 20, 110, 480));

        jLabel1.setText("PCB");
        jPanel4.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 0, -1, -1));

        jLabel2.setText("Salida");
        jPanel4.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 0, -1, -1));

        jLabel3.setText("Bloqueados");
        jPanel4.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 0, -1, -1));

        jLabel4.setText("Block-Suspendidos");
        jPanel4.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 240, -1, -1));

        jLabel6.setText("Reloj Global");
        jPanel4.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 250, -1, -1));

        relojglobal.setEditable(false);
        relojglobal.setText("0");
        jPanel4.add(relojglobal, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 270, 100, -1));

        jLabel8.setText("CPU ");
        jPanel4.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 300, -1, -1));

        bloqueadosSuspendidos.setEditable(false);
        bloqueadosSuspendidos.setColumns(20);
        bloqueadosSuspendidos.setRows(5);
        jScrollPane8.setViewportView(bloqueadosSuspendidos);

        jPanel4.add(jScrollPane8, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 260, 110, 240));

        listosSuspendidos.setEditable(false);
        listosSuspendidos.setColumns(20);
        listosSuspendidos.setRows(5);
        jScrollPane9.setViewportView(listosSuspendidos);

        jPanel4.add(jScrollPane9, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 20, 110, 210));

        jLabel5.setText("Listos");
        jPanel4.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 0, -1, -1));

        jLabel7.setText("Listos-Suspendidos");
        jPanel4.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 0, -1, -1));

        jTabbedPane1.addTab("Simulación", jPanel4);

        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        eventLogArea.setEditable(false);
        eventLogArea.setColumns(20);
        eventLogArea.setRows(5);
        eventLogArea.setFont(new java.awt.Font("Monospaced", 0, 12));
        jScrollPane11.setViewportView(eventLogArea);

        jPanel5.add(jScrollPane11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 710, 450));

        clearLogBtn.setText("Limpiar Log");
        clearLogBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogBtnActionPerformed(evt);
            }
        });
        jPanel5.add(clearLogBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 480, 120, 30));

        jTabbedPane1.addTab("Log de Eventos", jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 762, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>                        

    private void nombreActionPerformed(java.awt.event.ActionEvent evt) {                                        
    }                                       

    private void duracionActionPerformed(java.awt.event.ActionEvent evt) {                                         
    }                                        

    private void cicloexcepActionPerformed(java.awt.event.ActionEvent evt) {                                           
    }                                          

    private void duracionexcepActionPerformed(java.awt.event.ActionEvent evt) {                                               
    }                                              
    
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        try {
            if (cpu != null && reloj != null) {
                cpu.start();
                reloj.start();

                ProcesoJsonHandler.writeProcesosToJson(todos, "procesos.json");

                this.jButton2.setEnabled(false);
                this.guardarproceso.setEnabled(false);
                this.generarAleatoriosBtn.setEnabled(false);

            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Error: CPU o reloj no inicializados.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this, "Error al iniciar la simulación.");
        }
    }                                        

    private void guardarprocesoActionPerformed(java.awt.event.ActionEvent evt) {                                                   
        if (this.validateInputs()) {
            String nombreProceso = nombre.getText().trim();
            int duracionnt = Integer.parseInt(duracion.getText().trim());
            String tipo = (String) this.tipoproceso.getSelectedItem();
            
            int prioridad = 0;
            try {
                prioridad = Integer.parseInt(prioridadField.getText().trim());
                if (prioridad < 0 || prioridad > 4) {
                    javax.swing.JOptionPane.showMessageDialog(null, "La prioridad debe estar entre 0 y 4");
                    return;
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(null, "Prioridad inválida");
                return;
            }
            
            Proceso p;
            if (this.tipoproceso.getSelectedIndex() == 0) {
                p = new Proceso(
                        listolista.getSize(),
                        nombreProceso,
                        tipo,
                        duracionnt,
                        1,
                        1,
                        prioridad
                );
            } 
            else {
                int ciclo = Integer.parseInt(cicloexcep.getText().trim());
                int duracionciclp = Integer.parseInt(duracionexcep.getText().trim());
                p = new Proceso(
                        listolista.getSize(),
                        nombreProceso,
                        tipo,
                        duracionnt,
                        ciclo,
                        duracionciclp,
                        prioridad
                );
            }
            listolista.appendLast(p);
            todos.appendLast(p);
            this.uPcbs();
            limpiarCampos();
        } else {
            javax.swing.JOptionPane.showMessageDialog(null, "Error en los atributos del proceso");
        }
    }                                                  

    private void tipoprocesoActionPerformed(java.awt.event.ActionEvent evt) {                                            
        if(tipoproceso.getSelectedIndex() == 1){
            this.duracionexcep.setEnabled(true);
            this.cicloexcep.setEnabled(true);
        }else{
            this.duracionexcep.setEnabled(false);
            this.cicloexcep.setEnabled(false);
        }
    }                                           

    private void tiempoinstruccionStateChanged(javax.swing.event.ChangeEvent evt) {                                               
        this.jLabel16.setText(this.tiempoinstruccion.getValue()+" ms");
        int[] h = {this.tiempoinstruccion.getValue(),this.politica.getSelectedIndex()};
        ProcesoJsonHandler.saveToJson(h, "numbers.json");
    }                                              

    private void politicaActionPerformed(java.awt.event.ActionEvent evt) {                                         
        int[] h = {this.tiempoinstruccion.getValue(),this.politica.getSelectedIndex()};
        ProcesoJsonHandler.saveToJson(h, "numbers.json");
    }                                        

    private void pcbsCaretUpdate(javax.swing.event.CaretEvent evt) {                                 
    }                                

    private void politicaItemStateChanged(java.awt.event.ItemEvent evt) {                                          
    }                                         

    private void toggleChartButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                  
        toggleChart();
    }                                                 

    private void generarAleatoriosBtnActionPerformed(java.awt.event.ActionEvent evt) {                                                     
        generarProcesosAleatorios();
    }                                                    

    private void clearLogBtnActionPerformed(java.awt.event.ActionEvent evt) {                                            
        if (planificador != null && planificador.getLogger() != null) {
            planificador.getLogger().clearEvents();
            eventLogArea.setText("");
        }
    }                                           

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VistaSimulacion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VistaSimulacion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VistaSimulacion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VistaSimulacion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VistaSimulacion().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    private javax.swing.JTextArea bloqueados;
    private javax.swing.JTextArea bloqueadosSuspendidos;
    private javax.swing.JPanel chartContainerPanel;
    private javax.swing.JTextField cicloexcep;
    private javax.swing.JButton clearLogBtn;
    private javax.swing.JTextArea cpu1;
    private javax.swing.JTextField duracion;
    private javax.swing.JTextField duracionexcep;
    private javax.swing.JTextArea eventLogArea;
    private javax.swing.JButton generarAleatoriosBtn;
    private javax.swing.JButton guardarproceso;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea listos;
    private javax.swing.JTextArea listosSuspendidos;
    private javax.swing.JTextArea metricsArea;
    private javax.swing.JTextField nombre;
    private javax.swing.JTextArea pcbs;
    private javax.swing.JComboBox<String> politica;
    private javax.swing.JTextField prioridadField;
    private javax.swing.JTextField relojglobal;
    private javax.swing.JTextArea salida;
    private javax.swing.JSlider tiempoinstruccion;
    private javax.swing.JComboBox<String> tipoproceso;
    private javax.swing.JButton toggleChartButton;
    // End of variables declaration                   
}