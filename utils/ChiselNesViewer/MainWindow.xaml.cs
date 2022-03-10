﻿using ControlzEx.Theming;
using MahApps.Metro.Controls;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace ChiselNesViewer {
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : MetroWindow {
        public MainWindow() {
            InitializeComponent();

            ThemeManager.Current.ChangeTheme(this, "Dark.Blue");
            //ThemeManager.Current.ChangeTheme(this, "Light.Blue");
        }
        private void LaunchGitHubSite(object sender, RoutedEventArgs e) {
            // Launch the GitHub site...
        }

        private void DeployCupCakes(object sender, RoutedEventArgs e) {
            // deploy some CupCakes...
        }
    }
}
