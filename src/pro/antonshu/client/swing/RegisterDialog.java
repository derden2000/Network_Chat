package pro.antonshu.client.swing;

import pro.antonshu.client.Network;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class RegisterDialog extends JDialog {

    private Network network;
    private JTextField tfUsername;
    private JPasswordField tfPassword;
    private JPasswordField tfPasswordConfirm;
    private JLabel lbUsername;
    private JLabel lbPassword;
    private JLabel lbPasswordConfirm;
    private JButton btnRegister;
    private JButton btnCancel;
    private boolean registered;

    public RegisterDialog(Frame parent, Network network) {
        super(parent, "Регистрация", true);
        this.network = network;

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        lbUsername = new JLabel("Имя пользователя: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);

        tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);

        lbPassword = new JLabel("Пароль: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);

        tfPassword = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(tfPassword, cs);

        lbPasswordConfirm = new JLabel("Подтвердите пароль: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(lbPasswordConfirm, cs);

        tfPasswordConfirm = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(tfPasswordConfirm, cs);
        panel.setBorder(new LineBorder(Color.GRAY));

        btnRegister = new JButton("Зарегистрироваться");
        btnCancel = new JButton("Отмена");

        JPanel bp = new JPanel();
        bp.add(btnRegister);

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (String.valueOf(tfPassword.getPassword()).equals(String.valueOf(tfPasswordConfirm.getPassword()))){
                    network.register(tfUsername.getText(), String.valueOf(tfPassword.getPassword()));
                    registered = true;

                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Вы успешно зарегистрированы. Перезапустите приложение и войдите с Вашими данными",
                            "Регистрация",
                            JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                    }
                    else {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                                                "Пароли не совпадают. Проверьте соответсвие паролей.",
                                                "Регистрация",
                                                JOptionPane.ERROR_MESSAGE);
                                        return;
                     }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Ошибка сети",
                            "Регистрация",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dispose();
            }
        });

        bp.add(btnCancel);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registered = false;
                dispose();
            }
        });

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public boolean isRegistered() {
        return registered;
    }
}
